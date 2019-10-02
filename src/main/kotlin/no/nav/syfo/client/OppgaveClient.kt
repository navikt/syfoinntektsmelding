package no.nav.syfo.client

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
import no.nav.syfo.LoggingMeta
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.helpers.retry
import java.time.LocalDate
import log
import net.logstash.logback.argument.StructuredArguments.fields
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import io.ktor.client.engine.apache.Apache

@KtorExperimentalAPI
@Component
class OppgaveClient constructor (
        @Value("\${oppgavebehandling_url}") private val url: String,
        @Value("\${securitytokenservice.url}") private val tokenUrl: String,
        @Value("\${srvappserver.username}") private val username: String,
        @Value("\${srvappserver.password}") private val password: String
) {

    private val log = log()
    private val oidcClient = StsOidcClient(username, password, tokenUrl)
    private val httpClient = buildClient()

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

    private suspend fun opprettOppgave(opprettOppgaveRequest: OpprettOppgaveRequest, msgId: String): OpprettOppgaveResponse = retry("opprett_oppgave") {
        httpClient.post<OpprettOppgaveResponse>(url) {
            contentType(ContentType.Application.Json)
            val oidcToken = oidcClient.oidcToken()
            this.header("Authorization", "Bearer ${oidcToken.access_token}")
            this.header("X-Correlation-ID", msgId)
            body = opprettOppgaveRequest
        }
    }

    private suspend fun hentOppgave(oppgavetype: String, journalpostId: String, msgId: String): OppgaveResponse = retry("hent_oppgave") {
        httpClient.get<OppgaveResponse>(url) {
            val oidcToken = oidcClient.oidcToken()
            this.header("Authorization", "Bearer ${oidcToken.access_token}")
            this.header("X-Correlation-ID", msgId)
            parameter("tema", "SYM")
            parameter("oppgavetype", oppgavetype)
            parameter("journalpostId", journalpostId)
            parameter("statuskategori", "AAPEN")
            parameter("sorteringsrekkefolge", "ASC")
            parameter("sorteringsfelt", "FRIST")
            parameter("limit", "10")
        }
    }

    suspend fun opprettOppgave(
            sakId: String,
            journalpostId: String,
            tildeltEnhetsnr: String,
            aktoerId: String,
            gjelderUtland: Boolean,
            sykmeldingId: String,
            loggingMeta: LoggingMeta
    ): OppgaveResultat {
        val oppgaveResponse = hentOppgave(oppgavetype = "JFR", journalpostId = journalpostId, msgId = sykmeldingId)
        if (oppgaveResponse.antallTreffTotalt > 0) {
            log.info("Det finnes allerede journalføringsoppgave for journalpost $journalpostId, {}", fields(loggingMeta))
            return OppgaveResultat(oppgaveResponse.oppgaver.first().id, true)
        }
        var behandlingstype: String? = null
        if (gjelderUtland) {
            log.info("Gjelder utland, {}", fields(loggingMeta))
            behandlingstype = "ae0106"
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
        log.info("Oppretter journalføringsoppgave på enhet $tildeltEnhetsnr, {}", fields(loggingMeta))
        return OppgaveResultat(opprettOppgave(opprettOppgaveRequest, sykmeldingId).id, false)
    }

    suspend fun opprettFordelingsOppgave(
            journalpostId: String,
            tildeltEnhetsnr: String,
            gjelderUtland: Boolean,
            sykmeldingId: String,
            loggingMeta: LoggingMeta
    ): OppgaveResultat {
        val oppgaveResponse = hentOppgave(oppgavetype = "FDR", journalpostId = journalpostId, msgId = sykmeldingId)
        if (oppgaveResponse.antallTreffTotalt > 0) {
            log.info("Det finnes allerede fordelingsoppgave for journalpost $journalpostId, {}", fields(loggingMeta))
            return OppgaveResultat(oppgaveResponse.oppgaver.first().id, true)
        }
        var behandlingstype: String? = null
        if (gjelderUtland) {
            log.info("Gjelder utland, {}", fields(loggingMeta))
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
        log.info("Oppretter fordelingsoppgave på enhet $tildeltEnhetsnr, {}", fields(loggingMeta))
        return OppgaveResultat(opprettOppgave(opprettOppgaveRequest, sykmeldingId).id, false)
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
