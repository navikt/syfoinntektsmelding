package no.nav.syfo.client

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*
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
