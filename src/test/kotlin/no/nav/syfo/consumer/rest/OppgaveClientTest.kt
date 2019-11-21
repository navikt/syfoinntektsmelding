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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class OppgaveClientTest {

    @Mock
    private lateinit var oppgaveConfig: OppgaveConfig

    @Mock
    private lateinit var tokenConsumer: TokenConsumer

    @KtorExperimentalAPI
    private lateinit var oppgaveClient: OppgaveClient

    private val eksisterendeOppgaveId = 1234
    private val eksisterendeFordelingsOppgaveId = 5678

    @Before
    @KtorExperimentalAPI
    fun setUp() {
        `when`(oppgaveConfig.url).thenReturn("url")
        oppgaveClient = OppgaveClient(oppgaveConfig, tokenConsumer)
    }

    @Test
    @KtorExperimentalAPI
    fun henterEksisterendeOppgave() {
        runBlocking {
            val client = lagClientMockEngine(lagOppgaveResponse())
            oppgaveClient.setHttpClient(client)

            val resultat = oppgaveClient.opprettOppgave("sakId", "123", "tildeltEnhet", "aktoerid", false)

            assertThat(resultat.oppgaveId).isEqualTo(eksisterendeOppgaveId)
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

            assertThat(resultat.oppgaveId).isNotEqualTo(eksisterendeOppgaveId)
            assertThat(resultat.duplikat).isFalse()
            assertThat(requestVerdier?.journalpostId).isEqualTo("123")
            assertThat(requestVerdier?.oppgavetype).isEqualTo("JFR")
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

        assertThat(resultat.oppgaveId).isNotEqualTo(eksisterendeFordelingsOppgaveId)
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

        assertThat(resultat.oppgaveId).isEqualTo(eksisterendeFordelingsOppgaveId)
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
            id = eksisterendeOppgaveId,
            tildeltEnhetsnr = "22",
            aktoerId = "aktoerId",
            journalpostId = "journalpostId",
            saksreferanse = "saksreferanse",
            tema = "tema",
            oppgavetype = "JFR"
        )
    }

    private fun lagFordelingsOppgave(): Oppgave {
        return Oppgave(
            id = eksisterendeFordelingsOppgaveId,
            tildeltEnhetsnr = "1",
            aktoerId = null,
            journalpostId = "journalpostId",
            saksreferanse = "saksreferanse",
            tema = "tema",
            oppgavetype = "FDR"
        )
    }
}
