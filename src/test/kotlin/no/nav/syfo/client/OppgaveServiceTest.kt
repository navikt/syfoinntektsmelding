package no.nav.syfo.client

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.customObjectMapper
import no.nav.syfo.utsattoppgave.BehandlingsKategori
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

private const val OPPGAVE_ID = 1234
private const val FORDELINGSOPPGAVE_ID = 5678

class OppgaveServiceTest {

    private var metrikk = mockk<Metrikk>(relaxed = true)

    private lateinit var oppgaveService: OppgaveService

    @Test

    fun henterEksisterendeOppgave() {
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagOppgaveResponse()), metrikk) { "mockToken" }
            val resultat = oppgaveService.opprettOppgave("123", "aktoerid", BehandlingsKategori.REFUSJON_MED_DATO)
            assertThat(resultat.oppgaveId).isEqualTo(OPPGAVE_ID)
            assertThat(resultat.duplikat).isTrue
        }
    }

    @Test

    fun oppretterNyOppgave() {
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
            val resultat = oppgaveService.opprettOppgave("123", "aktoerid", BehandlingsKategori.REFUSJON_MED_DATO)
            val requestVerdier = hentRequestInnhold(oppgaveService.httpClient)
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
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
            val resultat = oppgaveService.opprettFordelingsOppgave("journalpostId")
            val requestVerdier = hentRequestInnhold(oppgaveService.httpClient)
            assertThat(resultat.oppgaveId).isNotEqualTo(FORDELINGSOPPGAVE_ID)
            assertThat(resultat.duplikat).isFalse
            assertThat(requestVerdier?.oppgavetype).isEqualTo("FDR")
            assertThat(requestVerdier?.behandlingstype).isNull()
        }
    }

    @Test

    fun henterEksisterendeFordelingsOppgave() {
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagFordelingsOppgaveResponse()), metrikk) { "mockToken" }
            val resultat = oppgaveService.opprettFordelingsOppgave("journalpostId")
            assertThat(resultat.oppgaveId).isEqualTo(FORDELINGSOPPGAVE_ID)
            assertThat(resultat.duplikat).isTrue
        }
    }

    @Test

    fun skal_opprette_utland() {
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
            oppgaveService.opprettOppgave("123", "aktoerid", BehandlingsKategori.UTLAND)
            val requestVerdier = hentRequestInnhold(oppgaveService.httpClient)
            assertThat(requestVerdier?.behandlingstype).isEqualTo("ae0106")
        }
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
            oppgaveService.opprettOppgave("123", "aktoerid", BehandlingsKategori.UTLAND)
            val requestVerdier = hentRequestInnhold(oppgaveService.httpClient)
            assertThat(requestVerdier?.behandlingstype).isEqualTo("ae0106")
        }
    }

    @Test
    fun skal_opprette_speil() {
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
            oppgaveService.opprettOppgave("123", "aktoerid", BehandlingsKategori.SPEIL_RELATERT)
            val requestVerdier = hentRequestInnhold(oppgaveService.httpClient)
            assertThat(requestVerdier?.behandlingstema).isEqualTo("ab0455")
        }
    }

    @Test
    fun skal_opprette_betviler_sykemelding() {
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
            oppgaveService.opprettOppgave("123", "aktoerid", BehandlingsKategori.BESTRIDER_SYKEMELDING)
            val requestVerdier = hentRequestInnhold(oppgaveService.httpClient)
            assertThat(requestVerdier?.behandlingstema).isEqualTo("ab0421")
            assertThat(requestVerdier?.behandlingstype).isEqualTo(null)
        }
    }

    @Test
    fun henterRiktigFerdigstillelsesFrist() {
        oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
        val onsdag = LocalDate.of(2019, Month.NOVEMBER, 27)
        val fredag = LocalDate.of(2019, Month.NOVEMBER, 29)
        val lørdag = LocalDate.of(2019, Month.NOVEMBER, 30)
        val søndag = LocalDate.of(2019, Month.DECEMBER, 1)

        assertThat(oppgaveService.leggTilEnVirkeuke(onsdag).dayOfWeek).isEqualTo(DayOfWeek.WEDNESDAY)
        assertThat(oppgaveService.leggTilEnVirkeuke(fredag).dayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
        assertThat(oppgaveService.leggTilEnVirkeuke(lørdag).dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(oppgaveService.leggTilEnVirkeuke(søndag).dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test

    fun skal_utbetale_til_bruker() {
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
            oppgaveService.opprettOppgave("123", "aktoerid", BehandlingsKategori.REFUSJON_MED_DATO)
            val requestVerdier = hentRequestInnhold(oppgaveService.httpClient)
            assertThat(requestVerdier?.behandlingstema).isEqualTo("ab0458")
        }
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
            oppgaveService.opprettOppgave("123", "aktoerid", BehandlingsKategori.IKKE_REFUSJON)
            val requestVerdier = hentRequestInnhold(oppgaveService.httpClient)
            assertThat(requestVerdier?.behandlingstema).isEqualTo("ab0458")
        }
        runBlocking {
            oppgaveService = OppgaveService("url", buildHttpClientJson(HttpStatusCode.OK, lagTomOppgaveResponse()), metrikk) { "mockToken" }
            oppgaveService.opprettOppgave("123", "aktoerid", BehandlingsKategori.REFUSJON_LITEN_LØNN)
            val requestVerdier = hentRequestInnhold(oppgaveService.httpClient)
            assertThat(requestVerdier?.behandlingstema).isEqualTo("ab0458")
        }
    }

    private suspend fun hentRequestInnhold(client: HttpClient): OpprettOppgaveRequest? {
        // TODO skriv om dette
        val requestHistorikk = (client.engine as MockEngine).requestHistory
        for (req in requestHistorikk) {

            if (req.method.value == "POST") {
                val mapper = customObjectMapper()
                return req.body.toByteArray().let { mapper.readValue(it) }
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
