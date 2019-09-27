package no.nav.syfo.client

import io.ktor.client.HttpClient
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

@KtorExperimentalAPI
class OppgaveClient constructor(private val url: String, private val oidcClient: StsOidcClient, private val httpClient: HttpClient) {

    private val log = log()

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
