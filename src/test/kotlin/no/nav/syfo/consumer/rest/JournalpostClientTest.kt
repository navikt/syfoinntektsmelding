package no.nav.syfo.consumer.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.ktor.util.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach

class JournalpostClientTest {

    private val mockStsClient = mockk<AccessTokenProvider>(relaxed = true)

    @KtorExperimentalAPI
    private lateinit var journalpostClient: JournalpostClient

    @org.junit.jupiter.api.Test
    @KtorExperimentalAPI
    fun skal_ferdigstille() {
        journalpostClient = JournalpostClient("http://localhost", mockStsClient, lagClientMockEngine(HttpStatusCode.OK) )
        runBlocking {
            val resultat = journalpostClient.ferdigstillJournalpost("111", "1001")
            Assertions.assertThat(resultat.value).isEqualTo(HttpStatusCode.OK.value)
        }
    }

    @org.junit.jupiter.api.Test
    @KtorExperimentalAPI
    fun skal_håndtere_ikke_ok() {
        journalpostClient = JournalpostClient("http://localhost", mockStsClient, lagClientMockEngine(HttpStatusCode.InternalServerError) )
        runBlocking {
            val resultat = journalpostClient.ferdigstillJournalpost("222", "2002")
            Assertions.assertThat(resultat.value).isEqualTo(HttpStatusCode.InternalServerError.value)
        }
    }

    private fun lagClientMockEngine(status: HttpStatusCode): HttpClient {
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
                    respond("", headers = headersOf("Content-Type" to listOf(ContentType.Text.Plain.toString())), status= status)
                }
            }
        }
    }

}
