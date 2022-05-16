package no.nav.syfo.client

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.BehandlingsTema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

private const val OPPGAVE_ID = 1234
private const val FORDELINGSOPPGAVE_ID = 5678

class OppgaveClientTest {

    private var tokenConsumer = mockk<TokenConsumer>(relaxed = true)
    private var metrikk = mockk<Metrikk>(relaxed = true)

    private lateinit var oppgaveClient: OppgaveClient

    @Test

    fun henterEksisterendeOppgave() {
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagOppgaveResponse()), metrikk)
            val resultat = oppgaveClient.opprettOppgave("123", "tildeltEnhet", "aktoerid", false, false, BehandlingsTema.REFUSJON_MED_DATO)
            assertThat(resultat.oppgaveId).isEqualTo(OPPGAVE_ID)
            assertThat(resultat.duplikat).isTrue
        }
    }

    @Test

    fun oppretterNyOppgave() {
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk)
            val resultat = oppgaveClient.opprettOppgave("123", "tildeltEnhet", "aktoerid", false, false, BehandlingsTema.REFUSJON_MED_DATO)
            val requestVerdier = hentRequestInnhold(oppgaveClient.httpClient)
            assertThat(resultat.oppgaveId).isNotEqualTo(OPPGAVE_ID)
            assertThat(resultat.duplikat).isFalse
            assertThat(requestVerdier?.journalpostId).isEqualTo("123")
            assertThat(requestVerdier?.oppgavetype).isEqualTo("INNT")
            assertThat(requestVerdier?.behandlingstype).isNull()
        }
    }

    @Test

    fun oppretterNyFordelingsOppgave() {
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk)
            val resultat = oppgaveClient.opprettFordelingsOppgave("journalpostId")
            val requestVerdier = hentRequestInnhold(oppgaveClient.httpClient)
            assertThat(resultat.oppgaveId).isNotEqualTo(FORDELINGSOPPGAVE_ID)
            assertThat(resultat.duplikat).isFalse
            assertThat(requestVerdier?.oppgavetype).isEqualTo("FDR")
            assertThat(requestVerdier?.behandlingstype).isNull()
        }
    }

    @Test

    fun henterEksisterendeFordelingsOppgave() {
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagFordelingsOppgaveResponse()), metrikk)
            val resultat = oppgaveClient.opprettFordelingsOppgave("journalpostId")
            assertThat(resultat.oppgaveId).isEqualTo(FORDELINGSOPPGAVE_ID)
            assertThat(resultat.duplikat).isTrue
        }
    }

    @Test

    fun skal_opprette_utland() {
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk)
            oppgaveClient.opprettOppgave("123", "tildeltEnhet", "aktoerid", true, false, BehandlingsTema.REFUSJON_MED_DATO)
            val requestVerdier = hentRequestInnhold(oppgaveClient.httpClient)
            assertThat(requestVerdier?.behandlingstype).isEqualTo("ae0106")
        }
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk)
            oppgaveClient.opprettOppgave("123", "tildeltEnhet", "aktoerid", true, false, BehandlingsTema.REFUSJON_UTEN_DATO)
            val requestVerdier = hentRequestInnhold(oppgaveClient.httpClient)
            assertThat(requestVerdier?.behandlingstype).isEqualTo("ae0106")
        }
    }

    @Test

    fun skal_opprette_speil() {
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk)
            oppgaveClient.opprettOppgave("123", "tildeltEnhet", "aktoerid", true, true, BehandlingsTema.REFUSJON_UTEN_DATO)
            val requestVerdier = hentRequestInnhold(oppgaveClient.httpClient)
            assertThat(requestVerdier?.behandlingstema).isEqualTo("ab0455")
        }
    }

    @Test

    fun henterRiktigFerdigstillelsesFrist() {
        oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk)
        val onsdag = LocalDate.of(2019, Month.NOVEMBER, 27)
        val fredag = LocalDate.of(2019, Month.NOVEMBER, 29)
        val lørdag = LocalDate.of(2019, Month.NOVEMBER, 30)
        val søndag = LocalDate.of(2019, Month.DECEMBER, 1)

        assertThat(oppgaveClient.leggTilEnVirkeuke(onsdag).dayOfWeek).isEqualTo(DayOfWeek.WEDNESDAY)
        assertThat(oppgaveClient.leggTilEnVirkeuke(fredag).dayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
        assertThat(oppgaveClient.leggTilEnVirkeuke(lørdag).dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(oppgaveClient.leggTilEnVirkeuke(søndag).dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test

    fun skal_utbetale_til_bruker() {
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk)
            oppgaveClient.opprettOppgave("123", "tildeltEnhet", "aktoerid", false, false, BehandlingsTema.REFUSJON_MED_DATO)
            val requestVerdier = hentRequestInnhold(oppgaveClient.httpClient)
            assertThat(requestVerdier?.behandlingstema).isEqualTo("ab0458")
        }
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk)
            oppgaveClient.opprettOppgave("123", "tildeltEnhet", "aktoerid", false, false, BehandlingsTema.IKKE_REFUSJON)
            val requestVerdier = hentRequestInnhold(oppgaveClient.httpClient)
            assertThat(requestVerdier?.behandlingstema).isEqualTo("ab0458")
        }
        runBlocking {
            oppgaveClient = OppgaveClient("url", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk)
            oppgaveClient.opprettOppgave("123", "tildeltEnhet", "aktoerid", false, false, BehandlingsTema.REFUSJON_LITEN_LØNN)
            val requestVerdier = hentRequestInnhold(oppgaveClient.httpClient)
            assertThat(requestVerdier?.behandlingstema).isEqualTo("ab0458")
        }
    }

    private fun hentRequestInnhold(client: HttpClient): OpprettOppgaveRequest? {
        val requestHistorikk = (client.engine as MockEngine).requestHistory
        for (req in requestHistorikk) {

            if (req.method.value == "POST") {
                val mapper = jacksonObjectMapper()
                mapper.registerKotlinModule()
                mapper.registerModule(JavaTimeModule())
                return mapper.readValue((req.body as TextContent).text)
            }
        }
        return null
    }

    private fun lagOppgaveResponse(): OppgaveResponse {
        return OppgaveResponse(
            antallTreffTotalt = 1,
            oppgaver = listOf(lagOppgave())
        )
    }

    private fun lagTomOppgaveResponse(): OppgaveResponse {
        return OppgaveResponse(
            antallTreffTotalt = 0,
            oppgaver = listOf()
        )
    }

    private fun lagFordelingsOppgaveResponse(): OppgaveResponse {
        return OppgaveResponse(
            antallTreffTotalt = 1,
            oppgaver = listOf(lagFordelingsOppgave())
        )
    }

    private fun lagOppgave(): Oppgave {
        return Oppgave(
            id = OPPGAVE_ID,
            tildeltEnhetsnr = "22",
            aktoerId = "aktoerId",
            journalpostId = "journalpostId",
            saksreferanse = "saksreferanse",
            tema = "tema",
            oppgavetype = "INNT"
        )
    }

    private fun lagFordelingsOppgave(): Oppgave {
        return Oppgave(
            id = FORDELINGSOPPGAVE_ID,
            tildeltEnhetsnr = "1",
            aktoerId = null,
            journalpostId = "journalpostId",
            saksreferanse = "saksreferanse",
            tema = "tema",
            oppgavetype = "FDR"
        )
    }
}
