package no.nav.syfo.consumer.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.config.OppgaveConfig
import no.nav.syfo.util.Metrikk
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.time.DayOfWeek
import java.time.Month

private const val OPPGAVE_ID = 1234
private const val FORDELINGSOPPGAVE_ID = 5678

@RunWith(MockitoJUnitRunner::class)
class OppgaveClientTest {

    @Mock
    private lateinit var oppgaveConfig: OppgaveConfig

    @Mock
    private lateinit var tokenConsumer: TokenConsumer

    @Mock
    private lateinit var metrikk: Metrikk

    @KtorExperimentalAPI
    private lateinit var oppgaveClient: OppgaveClient

    @Before
    @KtorExperimentalAPI
    fun setUp() {
        `when`(oppgaveConfig.url).thenReturn("url")
        oppgaveClient = OppgaveClient(oppgaveConfig, tokenConsumer, metrikk)
    }

    @Test
    @KtorExperimentalAPI
    fun henterEksisterendeOppgave() {
        runBlocking {
            val client = lagClientMockEngine(lagOppgaveResponse())
            oppgaveClient.setHttpClient(client)

            val resultat = oppgaveClient.opprettOppgave("sakId", "123", "tildeltEnhet", "aktoerid", false)

            assertThat(resultat.oppgaveId).isEqualTo(OPPGAVE_ID)
            assertThat(resultat.duplikat).isTrue()
        }
    }

    @Test
    @KtorExperimentalAPI
    fun oppretterNyOppgave() {
        runBlocking {
            val client = lagClientMockEngine(lagTomOppgaveResponse())
            oppgaveClient.setHttpClient(client)

            val resultat = oppgaveClient.opprettOppgave("sakId", "123", "tildeltEnhet", "aktoerid", false)
            val requestVerdier = hentRequestInnhold(client)

            assertThat(resultat.oppgaveId).isNotEqualTo(OPPGAVE_ID)
            assertThat(resultat.duplikat).isFalse()
            assertThat(requestVerdier?.journalpostId).isEqualTo("123")
            assertThat(requestVerdier?.oppgavetype).isEqualTo("INNT")
            assertThat(requestVerdier?.behandlingstype).isNull()
        }
    }

    @Test
    @KtorExperimentalAPI
    fun oppretterNyFordelingsOppgave() {
        runBlocking {
        val client = lagClientMockEngine(lagTomOppgaveResponse())
        oppgaveClient.setHttpClient(client)

        val resultat = oppgaveClient.opprettFordelingsOppgave("journalpostId", "1", false)
        val requestVerdier = hentRequestInnhold(client)

        assertThat(resultat.oppgaveId).isNotEqualTo(FORDELINGSOPPGAVE_ID)
        assertThat(resultat.duplikat).isFalse()
        assertThat(requestVerdier?.oppgavetype).isEqualTo("FDR")
        assertThat(requestVerdier?.behandlingstype).isNull()
        }
    }

    @Test
    @KtorExperimentalAPI
    fun henterEksisterendeFordelingsOppgave() {
        runBlocking {
        val client = lagClientMockEngine(lagFordelingsOppgaveResponse())
        oppgaveClient.setHttpClient(client)

        val resultat = oppgaveClient.opprettFordelingsOppgave("journalpostId", "1", false)

        assertThat(resultat.oppgaveId).isEqualTo(FORDELINGSOPPGAVE_ID)
        assertThat(resultat.duplikat).isTrue()
        }
    }

    @Test
    @KtorExperimentalAPI
    fun gjelderUtlandFårBehandlingstype() {
        runBlocking {
        val client = lagClientMockEngine(lagTomOppgaveResponse())
        oppgaveClient.setHttpClient(client)

        oppgaveClient.opprettOppgave("sakId", "123", "tildeltEnhet", "aktoerid", true)
        val requestVerdier = hentRequestInnhold(client)

        assertThat(requestVerdier?.behandlingstype).isEqualTo("ae0106")
        }
    }

    @Test
    @KtorExperimentalAPI
    fun henterRiktigFerdigstillelsesFrist() {
        val onsdag = LocalDate.of(2019, Month.NOVEMBER, 27)
        val fredag = LocalDate.of(2019, Month.NOVEMBER, 29)
        val lørdag =  LocalDate.of(2019, Month.NOVEMBER, 30)
        val søndag =  LocalDate.of(2019, Month.DECEMBER, 1)

        assertThat(oppgaveClient.leggTilEnVirkeuke(onsdag).dayOfWeek).isEqualTo(DayOfWeek.WEDNESDAY)
        assertThat(oppgaveClient.leggTilEnVirkeuke(fredag).dayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
        assertThat(oppgaveClient.leggTilEnVirkeuke(lørdag).dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(oppgaveClient.leggTilEnVirkeuke(søndag).dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
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

    private fun lagClientMockEngine(oppgaveResponse: OppgaveResponse): HttpClient{
        return HttpClient(MockEngine) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
                expectSuccess = false
            }
            engine {
                addHandler {
                    respond(jacksonObjectMapper().writeValueAsString(oppgaveResponse), headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())))
                }
            }
        }
    }


    private fun lagOppgaveResponse(): OppgaveResponse {
        return OppgaveResponse (
            antallTreffTotalt = 1,
            oppgaver = listOf(lagOppgave())
        )
    }

    private fun lagTomOppgaveResponse(): OppgaveResponse {
        return OppgaveResponse (
            antallTreffTotalt = 0,
            oppgaver = listOf()
        )
    }

    private fun lagFordelingsOppgaveResponse(): OppgaveResponse {
        return OppgaveResponse (
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
