package no.nav.syfo.consumer.rest.dokarkiv

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.http.*

fun buildHttpClientText(status: HttpStatusCode, text: String = ""): HttpClient {
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
                respond(
                    text,
                    headers = headersOf("Content-Type" to listOf(ContentType.Text.Plain.toString())),
                    status = status
                )
            }
        }
    }
}

fun buildHttpClientJson(status: HttpStatusCode, json: String = "{}"): HttpClient {
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
                respond(
                    json,
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    status = status
                )
            }
        }
    }
}
