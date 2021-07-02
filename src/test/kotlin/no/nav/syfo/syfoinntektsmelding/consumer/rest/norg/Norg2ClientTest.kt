package no.nav.syfo.syfoinntektsmelding.consumer.rest.norg

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.ktor.util.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.consumer.rest.norg.ArbeidsfordelingRequest
import no.nav.syfo.consumer.rest.norg.ArbeidsfordelingResponse
import no.nav.syfo.consumer.rest.norg.Norg2Client
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Norg2ClientTest {

    @KtorExperimentalAPI
    private lateinit var norgClient: Norg2Client

    private val mockStsClient = mockk<AccessTokenProvider>(relaxed = true)

    @Test
    @KtorExperimentalAPI
    fun hentAlleArbeidsfordelinger() {
        runBlocking {
            norgClient = Norg2Client("url", mockStsClient, mockHttpClientWithResponse(lagResponse()))
            val arbeidsfordelinger = norgClient.hentAlleArbeidsfordelinger(lagRequest(), "123")
            assertThat(arbeidsfordelinger.size).isEqualTo(1)
            assertThat(arbeidsfordelinger[0].gyldigFra).isEqualTo(LocalDate.of(2021, 1, 1))
        }
    }

    private fun mockHttpClientWithResponse(oppgaveResponse: List<ArbeidsfordelingResponse>): HttpClient {
        return HttpClient(MockEngine) {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    registerModule(KotlinModule())
                    registerModule(Jdk8Module())
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    configure(SerializationFeature.INDENT_OUTPUT, true)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                    accept(ContentType.Application.Json)
                }
                expectSuccess = false
            }
            engine {
                addHandler {
                    respond(
                        jacksonObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(oppgaveResponse),
                        headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    )
                }
            }
        }
    }

    private fun lagResponse(): List<ArbeidsfordelingResponse> {
        val a = ArbeidsfordelingResponse(
            behandlingstema = "string",
            behandlingstype = "string",
            diskresjonskode = "string",
            enhetId = 0,
            enhetNavn = "string",
            enhetNr = "string",
            geografiskOmraade = "string",
            gyldigFra = LocalDate.of(2021, 1, 1),
            gyldigTil = LocalDate.of(2021, 11, 1),
            id = 0,
            oppgavetype = "string",
            skalTilLokalkontor = true,
            tema = "string",
            temagruppe = "string"
        )
        return listOf(a)
    }

    private fun lagRequest(): ArbeidsfordelingRequest {
        return ArbeidsfordelingRequest(
            tema = "tema",
            oppgavetype = "INNT"
        )
    }

}
