package no.nav.syfo.consumer.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import log
import no.nav.syfo.behandling.HentOppgaveException
import no.nav.syfo.behandling.OpprettFordelingsOppgaveException
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.helpers.retry
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.Metrikk
import java.time.DayOfWeek
import java.time.LocalDate

const val OPPGAVETYPE_INNTEKTSMELDING = "INNT"
const val OPPGAVETYPE_FORDELINGSOPPGAVE = "FDR"
const val TEMA = "SYK"

class OppgaveClient constructor (
        val oppgavebehndlingUrl: String,
        val tokenConsumer: TokenConsumer,
        val metrikk: Metrikk
) {

    private val log = log()
    private var httpClient = buildClient()

    fun setHttpClient(httpClient: HttpClient) {
        this.httpClient = httpClient
    }

    private fun buildClient(): HttpClient {
        return HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
                expectSuccess = false
            }
        }
    }

    private suspend fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest): OpprettOppgaveResponse = retry("opprett_oppgave") {
        httpClient.post<OpprettOppgaveResponse>(oppgavebehndlingUrl) {
            contentType(ContentType.Application.Json)
            this.header("Authorization", "Bearer ${tokenConsumer.token}")
            this.header("X-Correlation-ID", MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID))
            body = opprettOppgaveRequest
        }
    }

    private suspend fun hentOppgave(oppgavetype: String, journalpostId: String): OppgaveResponse {
        return retry("hent_oppgave") {
            val callId = MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID)
            log.info("Henter oppgave med CallId $callId")
            httpClient.get<OppgaveResponse>(oppgavebehndlingUrl) {
                this.header("Authorization", "Bearer ${tokenConsumer.token}")
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

    suspend fun hentHvisOppgaveFinnes(oppgavetype: String, journalpostId: String): OppgaveResultat? {
        try {
            val oppgaveResponse = hentOppgave(oppgavetype = oppgavetype, journalpostId = journalpostId)
            return if (oppgaveResponse.antallTreffTotalt > 0) OppgaveResultat(oppgaveResponse.oppgaver.first().id, true) else null
        } catch (ex: Exception) {
            log.error("Feil ved sjekking av eksisterende oppgave", ex)
            throw HentOppgaveException(journalpostId, oppgavetype, ex)
        }
    }

    suspend fun opprettOppgave(
        sakId: String,
        journalpostId: String,
        tildeltEnhetsnr: String,
        aktoerId: String,
        gjelderUtland: Boolean
    ): OppgaveResultat {
        val eksisterendeOppgave = hentHvisOppgaveFinnes(OPPGAVETYPE_INNTEKTSMELDING, journalpostId)
        metrikk.tellOpprettOppgave(eksisterendeOppgave != null)
        if (eksisterendeOppgave != null) {
            log.info("Det finnes allerede journalføringsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        var behandlingstype: String? = null
        var behandlingstema: String? = null

        if (gjelderUtland) {
            log.info("Gjelder utland")
            behandlingstype = "ae0106"
        } else {
            behandlingstema = "ab0061"
        }

        val opprettOppgaveRequest = OpprettOppgaveRequest(
            tildeltEnhetsnr = tildeltEnhetsnr,
            aktoerId = aktoerId,
            journalpostId = journalpostId,
            behandlesAvApplikasjon = "FS22",
            saksreferanse = sakId,
            beskrivelse = "Det har kommet en inntektsmelding på sykepenger.",
            tema = TEMA,
            oppgavetype = OPPGAVETYPE_INNTEKTSMELDING,
            behandlingstype = behandlingstype,
            behandlingstema = behandlingstema,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = leggTilEnVirkeuke(LocalDate.now()),
            prioritet = "NORM"
        )
        log.info("Oppretter journalføringsoppgave på enhet $tildeltEnhetsnr")
        try {
            return OppgaveResultat(opprettOppgave(opprettOppgaveRequest).id, false)
        } catch (ex : Exception) {
            throw OpprettOppgaveException(journalpostId, ex)
        }
    }

    suspend fun opprettFordelingsOppgave(
        journalpostId: String
    ): OppgaveResultat {

        val eksisterendeOppgave = hentHvisOppgaveFinnes(OPPGAVETYPE_FORDELINGSOPPGAVE, journalpostId)

        if (eksisterendeOppgave != null) {
            log.info("Det finnes allerede fordelingsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        var behandlingstype: String? = null

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
        log.info("Oppretter fordelingsoppgave")
        try {
            return OppgaveResultat(opprettOppgave(opprettOppgaveRequest).id, false)
        } catch (ex : Exception) {
            throw OpprettFordelingsOppgaveException(journalpostId, ex)
        }
    }

    fun leggTilEnVirkeuke(dato : LocalDate) : LocalDate {
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
