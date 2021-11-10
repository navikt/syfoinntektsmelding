package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.syfo.koin.buildJacksonSerializer
import no.nav.syfo.koin.buildObjectMapper

fun buildHttpClientText(status: HttpStatusCode, text: String = ""): HttpClient {
    return HttpClient(MockEngine) {
        install(JsonFeature) {
            serializer = buildJacksonSerializer()
            expectSuccess = false
        }
        engine {
            addHandler {
                respond(
                    text,
                    headers = headersOf("Content-Type" to listOf(ContentType.Text.Plain.toString())),
                    status = status
                )
            }
        }
    }
}

fun buildHttpClientJson(status: HttpStatusCode, response: Any): HttpClient {
    return HttpClient(MockEngine) {
        install(JsonFeature) {
            serializer = buildJacksonSerializer()
            expectSuccess = false
        }
        engine {
            addHandler {
                respond(
                    buildObjectMapper().writeValueAsString(response),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    status = status
                )
            }
        }
    }
}
