package no.nav.syfo.client

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import no.nav.syfo.koin.buildObjectMapper
import no.nav.syfo.util.customObjectMapper

fun buildHttpClientText(
    status: HttpStatusCode,
    text: String = "",
): HttpClient {
    val mockEngine =
        MockEngine {
            respond(
                text,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
            )
        }
    return HttpClient(mockEngine) {
        expectSuccess = true

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(customObjectMapper()))
            jackson {
                registerModule(JavaTimeModule())
            }
        }
    }
}

fun buildHttpClientJson(
    status: HttpStatusCode,
    response: Any,
): HttpClient {
    return HttpClient(MockEngine) {
        expectSuccess = true
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(customObjectMapper()))
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        engine {
            addHandler {
                respond(
                    buildObjectMapper().writeValueAsString(response),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    status = status,
                )
            }
        }
    }
}
