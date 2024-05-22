package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.behandling.HentOppgaveException
import no.nav.syfo.behandling.OpprettFordelingsOppgaveException
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.helpers.retry
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.BehandlingsKategori
import java.time.DayOfWeek
import java.time.LocalDate

const val OPPGAVETYPE_INNTEKTSMELDING = "INNT"
const val OPPGAVETYPE_FORDELINGSOPPGAVE = "FDR"
const val TEMA = "SYK"
const val BEHANDLINGSTEMA_SPEIL = "ab0455"
const val BEHANDLINGSTEMA_UTBETALING_TIL_BRUKER = "ab0458"
const val BEHANDLINGSTYPE_UTLAND = "ae0106"
const val BEHANDLINGSTYPE_NORMAL = "ab0061"
const val BEHANDLINGSTEMA_BETVILER_SYKEMELDING = "ab0421"

class OppgaveClient(
    val oppgavebehandlingUrl: String,
    val httpClient: HttpClient,
    val metrikk: Metrikk,
    val getAccessToken: () -> String
) {
    private val logger = this.logger()
    private val sikkerlogger = sikkerLogger()

    private suspend fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): OpprettOppgaveResponse = retry("opprett_oppgave") {
        httpClient.post<OpprettOppgaveResponse>(oppgavebehandlingUrl) {
            contentType(ContentType.Application.Json)
            this.header("Authorization", "Bearer ${getAccessToken()}")
            this.header("X-Correlation-ID", MdcUtils.getCallId())
            body = opprettOppgaveRequest
        }
    }

    private suspend fun hentOppgave(oppgavetype: String, journalpostId: String): OppgaveResponse {
        return retry("hent_oppgave") {
            val callId = MdcUtils.getCallId()
            logger.info("Henter oppgave med CallId $callId")
            httpClient.get<OppgaveResponse>(oppgavebehandlingUrl) {
                this.header("Authorization", "Bearer ${getAccessToken()}")
                this.header("X-Correlation-ID", callId)
                parameter("tema", TEMA)
                parameter("oppgavetype", oppgavetype)
                parameter("journalpostId", journalpostId)
                parameter("statuskategori", "AAPEN")
                parameter("sorteringsrekkefolge", "ASC")
                parameter("sorteringsfelt", "FRIST")
                parameter("limit", "10")
            }
        }
    }

    private suspend fun hentHvisOppgaveFinnes(oppgavetype: String, journalpostId: String): OppgaveResultat? {
        try {
            val oppgaveResponse = hentOppgave(oppgavetype = oppgavetype, journalpostId = journalpostId)
            return if (oppgaveResponse.antallTreffTotalt > 0) OppgaveResultat(oppgaveResponse.oppgaver.first().id, true, false) else null
        } catch (ex: Exception) {
            sikkerlogger.error("Feil ved sjekking av eksisterende oppgave", ex)
            throw HentOppgaveException(journalpostId, oppgavetype, ex)
        }
    }

    suspend fun opprettOppgave(
        journalpostId: String,
        tildeltEnhetsnr: String?,
        aktoerId: String,
        behandlingsKategori: BehandlingsKategori
    ): OppgaveResultat {

        val eksisterendeOppgave = hentHvisOppgaveFinnes(OPPGAVETYPE_INNTEKTSMELDING, journalpostId)
        metrikk.tellOpprettOppgave(eksisterendeOppgave != null)
        if (eksisterendeOppgave != null) {
            logger.info("Det finnes allerede journalføringsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        fun loggOppgave(type: String) {
            logger.info("Oppretter oppgave: $type for journalpost $journalpostId")
        }

        val (behandlingstype, behandlingstema, utbetalingBruker) = when {
            behandlingsKategori == BehandlingsKategori.SPEIL_RELATERT -> {
                loggOppgave("Speil")
                Triple(null, BEHANDLINGSTEMA_SPEIL, false)
            }
            behandlingsKategori == BehandlingsKategori.UTLAND -> {
                loggOppgave("Utland")
                Triple(BEHANDLINGSTYPE_UTLAND, null, false)
            }
            behandlingsKategori == BehandlingsKategori.BETVILER_SYKEMELDING -> {
                loggOppgave("Betviler sykmelding")
                Triple(null, BEHANDLINGSTEMA_BETVILER_SYKEMELDING, false)
            }
            // TODO flip condition ??
            behandlingsKategori != BehandlingsKategori.REFUSJON_UTEN_DATO -> {
                loggOppgave("Utbetaling til bruker")
                Triple(null, BEHANDLINGSTEMA_UTBETALING_TIL_BRUKER, true)
            }
            else -> {
                loggOppgave("Normal")
                Triple(null, BEHANDLINGSTYPE_NORMAL, false)
            }
        }

        val opprettOppgaveRequest = OpprettOppgaveRequest(
            tildeltEnhetsnr = tildeltEnhetsnr,
            aktoerId = aktoerId,
            journalpostId = journalpostId,
            behandlesAvApplikasjon = "FS22",
            beskrivelse = "Det har kommet en inntektsmelding på sykepenger.",
            tema = "SYK",
            oppgavetype = OPPGAVETYPE_INNTEKTSMELDING,
            behandlingstype = behandlingstype,
            behandlingstema = behandlingstema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = leggTilEnVirkeuke(LocalDate.now()),
            prioritet = "NORM"
        )
        logger.info("Oppretter journalføringsoppgave på enhet $tildeltEnhetsnr")
        try {
            return OppgaveResultat(opprettOppgave(opprettOppgaveRequest).id, false, utbetalingBruker)
        } catch (ex: Exception) {
            throw OpprettOppgaveException(journalpostId, ex)
        }
    }

    suspend fun opprettFordelingsOppgave(
        journalpostId: String
    ): OppgaveResultat {

        val eksisterendeOppgave = hentHvisOppgaveFinnes(OPPGAVETYPE_FORDELINGSOPPGAVE, journalpostId)

        if (eksisterendeOppgave != null) {
            logger.info("Det finnes allerede fordelingsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        val behandlingstype: String? = null

        val opprettOppgaveRequest = OpprettOppgaveRequest(
            journalpostId = journalpostId,
            behandlesAvApplikasjon = "FS22",
            beskrivelse = "Fordelingsoppgave for inntektsmelding på sykepenger",
            tema = TEMA,
            oppgavetype = OPPGAVETYPE_FORDELINGSOPPGAVE,
            behandlingstype = behandlingstype,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = leggTilEnVirkeuke(LocalDate.now()),
            prioritet = "NORM"
        )
        logger.info("Oppretter fordelingsoppgave")
        try {
            return OppgaveResultat(opprettOppgave(opprettOppgaveRequest).id, false, false)
        } catch (ex: Exception) {
            throw OpprettFordelingsOppgaveException(journalpostId, ex)
        }
    }

    fun leggTilEnVirkeuke(dato: LocalDate): LocalDate {
        return when (dato.dayOfWeek) {
            DayOfWeek.SATURDAY -> dato.plusDays(9)
            DayOfWeek.SUNDAY -> dato.plusDays(8)
            else -> dato.plusDays(7)
        }
    }
}

data class OpprettOppgaveRequest(
    val tildeltEnhetsnr: String? = null,
    val aktoerId: String? = null,
    val journalpostId: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val saksreferanse: String? = null,
    val beskrivelse: String? = null,
    val tema: String? = null,
    val oppgavetype: String,
    val behandlingstype: String? = null,
    val behandlingstema: String? = null,
    val aktivDato: LocalDate,
    val fristFerdigstillelse: LocalDate? = null,
    val prioritet: String
)

data class OpprettOppgaveResponse(
    val id: Int
)

data class OppgaveResponse(
    val antallTreffTotalt: Int,
    val oppgaver: List<Oppgave>
)

data class Oppgave(
    val id: Int,
    val tildeltEnhetsnr: String?,
    val aktoerId: String?,
    val journalpostId: String?,
    val saksreferanse: String?,
    val tema: String?,
    val oppgavetype: String?
)
