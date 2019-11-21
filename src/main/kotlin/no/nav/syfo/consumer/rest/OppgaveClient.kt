package no.nav.syfo.consumer.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.helpers.retry
import java.time.LocalDate
import log
import org.springframework.stereotype.Component
import io.ktor.client.engine.apache.Apache
import no.nav.syfo.config.OppgaveConfig
import no.nav.syfo.util.MDCOperations


@Component
class OppgaveClient constructor (
        val config: OppgaveConfig,
        val tokenConsumer: TokenConsumer
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
        httpClient.post<OpprettOppgaveResponse>(config.url) {
            contentType(ContentType.Application.Json)
            this.header("Authorization", "Bearer ${tokenConsumer.token}")
            this.header("X-Correlation-ID", MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID))
            body = opprettOppgaveRequest
        }
    }

    private suspend fun hentOppgave(oppgavetype: String, journalpostId: String): OppgaveResponse = retry("hent_oppgave") {
        httpClient.get<OppgaveResponse>(config.url) {
            this.header("Authorization", "Bearer ${tokenConsumer.token}")
            this.header("X-Correlation-ID", MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID))
            parameter("tema", "SYM")
            parameter("oppgavetype", oppgavetype)
            parameter("journalpostId", journalpostId)
            parameter("statuskategori", "AAPEN")
            parameter("sorteringsrekkefolge", "ASC")
            parameter("sorteringsfelt", "FRIST")
            parameter("limit", "10")
        }
    }

    suspend fun hentHvisOppgaveFinnes(
        oppgavetype: String,
        journalpostId: String
    ): OppgaveResultat? {
        val oppgaveResponse = hentOppgave(oppgavetype = oppgavetype, journalpostId = journalpostId)
        return if (oppgaveResponse.antallTreffTotalt > 0) OppgaveResultat(oppgaveResponse.oppgaver.first().id, true) else null
    }

    suspend fun opprettOppgave(
        sakId: String,
        journalpostId: String,
        tildeltEnhetsnr: String,
        aktoerId: String,
        gjelderUtland: Boolean
    ): OppgaveResultat {
        val eksisterendeOppgave = hentHvisOppgaveFinnes("JFR", journalpostId)

        if (eksisterendeOppgave != null) {
            log.info("Det finnes allerede journalføringsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        val behandlingstype = if (gjelderUtland) {
            log.info("Gjelder utland")
            "ae0106"
        } else {
            null
        }

        val opprettOppgaveRequest = OpprettOppgaveRequest(
            tildeltEnhetsnr = tildeltEnhetsnr,
            aktoerId = aktoerId,
            opprettetAvEnhetsnr = "9999",
            journalpostId = journalpostId,
            behandlesAvApplikasjon = "FS22",
            saksreferanse = sakId,
            beskrivelse = "Papirsykmelding som må legges inn i infotrygd manuelt",
            tema = "SYM",
            oppgavetype = "JFR",
            behandlingstype = behandlingstype,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(1),
            prioritet = "NORM"
        )
        log.info("Oppretter journalføringsoppgave på enhet $tildeltEnhetsnr")
        return OppgaveResultat(opprettOppgave(opprettOppgaveRequest).id, false)
    }

    suspend fun opprettFordelingsOppgave(
        journalpostId: String,
        tildeltEnhetsnr: String,
        gjelderUtland: Boolean
    ): OppgaveResultat {

        val eksisterendeOppgave = hentHvisOppgaveFinnes("FDR", journalpostId)

        if (eksisterendeOppgave != null) {
            log.info("Det finnes allerede fordelingsoppgave for journalpost $journalpostId")
            return eksisterendeOppgave
        }

        var behandlingstype: String? = null
        if (gjelderUtland) {
            log.info("Gjelder utland")
            behandlingstype = "ae0106"
        }
        val opprettOppgaveRequest = OpprettOppgaveRequest(
            tildeltEnhetsnr = tildeltEnhetsnr,
            opprettetAvEnhetsnr = "9999",
            journalpostId = journalpostId,
            behandlesAvApplikasjon = "FS22",
            beskrivelse = "Fordelingsoppgave for mottatt papirsykmelding som må legges inn i infotrygd manuelt",
            tema = "SYM",
            oppgavetype = "FDR",
            behandlingstype = behandlingstype,
            aktivDato = LocalDate.now(),
            fristFerdigstillelse = LocalDate.now().plusDays(1),
            prioritet = "NORM"
        )
        log.info("Oppretter fordelingsoppgave på enhet $tildeltEnhetsnr")
        return OppgaveResultat(opprettOppgave(opprettOppgaveRequest).id, false)
    }
}

data class OpprettOppgaveRequest(
        val tildeltEnhetsnr: String? = null,
        val opprettetAvEnhetsnr: String? = null,
        val aktoerId: String? = null,
        val journalpostId: String? = null,
        val behandlesAvApplikasjon: String? = null,
        val saksreferanse: String? = null,
        val tilordnetRessurs: String? = null,
        val beskrivelse: String? = null,
        val tema: String? = null,
        val oppgavetype: String,
        val behandlingstype: String? = null,
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
