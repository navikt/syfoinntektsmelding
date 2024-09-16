package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.behandling.HentOppgaveException
import no.nav.syfo.behandling.OpprettFordelingsOppgaveException
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.BehandlingsKategori
import java.time.DayOfWeek
import java.time.LocalDate

const val TEMA = "SYK"

object Oppgavetype {
    const val INNTEKTSMELDING = "INNT"
    const val FORDELINGSOPPGAVE = "FDR"
}

object Behandlingstype {
    const val UTLAND = "ae0106"
}

object Behandlingstema {
    const val NORMAL = "ab0061"
    const val SPEIL = "ab0455"
    const val UTBETALING_TIL_BRUKER = "ab0458"
    const val BESTRIDER_SYKEMELDING = "ab0421"
}

class OppgaveClient(
    val oppgavebehandlingUrl: String,
    val httpClient: HttpClient,
    val metrikk: Metrikk,
    val getAccessToken: () -> String
) {
    private val logger = this.logger()
    private val sikkerlogger = sikkerLogger()

    private suspend fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): OpprettOppgaveResponse {
        val httpResponse =
            httpClient.post(oppgavebehandlingUrl) {
                contentType(ContentType.Application.Json)
                this.header("Authorization", "Bearer ${getAccessToken()}")
                this.header("X-Correlation-ID", MdcUtils.getCallId())
                setBody(opprettOppgaveRequest)
            }
        return httpResponse.call.response.body()
    }

    private suspend fun hentOppgave(
        oppgavetype: String,
        journalpostId: String,
    ): OppgaveResponse {
        val stsToken = getAccessToken()
        val callId = MdcUtils.getCallId()
        val httpResponse =
            httpClient.get(oppgavebehandlingUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $stsToken")
                header("X-Correlation-ID", callId)
                parameter("tema", TEMA)
                parameter("oppgavetype", oppgavetype)
                parameter("journalpostId", journalpostId)
                parameter("statuskategori", "AAPEN")
                parameter("sorteringsrekkefolge", "ASC")
                parameter("sorteringsfelt", "FRIST")
                parameter("limit", "10")
            }
        return httpResponse.call.response.body()
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
        aktoerId: String,
        behandlingsKategori: BehandlingsKategori
    ): OppgaveResultat {

        val eksisterendeOppgave = hentHvisOppgaveFinnes(Oppgavetype.INNTEKTSMELDING, journalpostId)
        metrikk.tellOpprettOppgave(eksisterendeOppgave != null)
        if (eksisterendeOppgave != null) {
            sikkerlogger.info("Det finnes allerede journalføringsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        fun loggOppgave(type: String) {
            sikkerlogger.info("Oppretter oppgave: $type for journalpost $journalpostId")
        }

        val (behandlingstype, behandlingstema, utbetalingBruker) = when (behandlingsKategori) {
            BehandlingsKategori.SPEIL_RELATERT -> {
                loggOppgave("Speil")
                Triple(null, Behandlingstema.SPEIL, false)
            }
            BehandlingsKategori.UTLAND -> {
                loggOppgave("Utland")
                Triple(Behandlingstype.UTLAND, null, false)
            }
            BehandlingsKategori.BESTRIDER_SYKEMELDING -> {
                loggOppgave("Bestrider sykmelding")
                Triple(null, Behandlingstema.BESTRIDER_SYKEMELDING, false)
            }
            BehandlingsKategori.IKKE_REFUSJON,
            BehandlingsKategori.REFUSJON_MED_DATO,
            BehandlingsKategori.REFUSJON_LITEN_LØNN -> {
                loggOppgave("Utbetaling til bruker")
                Triple(null, Behandlingstema.UTBETALING_TIL_BRUKER, true)
            }
            else -> {
                loggOppgave("Normal")
                Triple(null, Behandlingstema.NORMAL, false)
            }
        }

        val opprettOppgaveRequest = OpprettOppgaveRequest(
            aktoerId = aktoerId,
            journalpostId = journalpostId,
            behandlesAvApplikasjon = "FS22",
            beskrivelse = "Det har kommet en inntektsmelding på sykepenger.",
            tema = "SYK",
            oppgavetype = Oppgavetype.INNTEKTSMELDING,
            behandlingstype = behandlingstype,
            behandlingstema = behandlingstema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = leggTilEnVirkeuke(LocalDate.now()),
            prioritet = "NORM"
        )
        sikkerlogger.info("Oppretter journalføringsoppgave")
        try {
            return OppgaveResultat(opprettOppgave(opprettOppgaveRequest).id, false, utbetalingBruker)
        } catch (ex: Exception) {
            throw OpprettOppgaveException(journalpostId, ex)
        }
    }

    suspend fun opprettFordelingsOppgave(journalpostId: String): OppgaveResultat {
        val eksisterendeOppgave = hentHvisOppgaveFinnes(Oppgavetype.FORDELINGSOPPGAVE, journalpostId)

        if (eksisterendeOppgave != null) {
            sikkerlogger.info("Det finnes allerede fordelingsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        val behandlingstype: String? = null

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                journalpostId = journalpostId,
                behandlesAvApplikasjon = "FS22",
                beskrivelse = "Fordelingsoppgave for inntektsmelding på sykepenger",
                tema = TEMA,
                oppgavetype = Oppgavetype.FORDELINGSOPPGAVE,
                behandlingstype = behandlingstype,
                aktivDato = LocalDate.now(),
                fristFerdigstillelse = leggTilEnVirkeuke(LocalDate.now()),
                prioritet = "NORM",
            )
        logger.info("Oppretter fordelingsoppgave")
        try {
            return OppgaveResultat(opprettOppgave(opprettOppgaveRequest).id, false, false)
        } catch (ex: Exception) {
            throw OpprettFordelingsOppgaveException(journalpostId, ex)
        }
    }

    fun leggTilEnVirkeuke(dato: LocalDate): LocalDate =
        when (dato.dayOfWeek) {
            DayOfWeek.SATURDAY -> dato.plusDays(9)
            DayOfWeek.SUNDAY -> dato.plusDays(8)
            else -> dato.plusDays(7)
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
