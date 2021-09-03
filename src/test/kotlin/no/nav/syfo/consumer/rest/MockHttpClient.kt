package no.nav.syfo.consumer.rest.dokarkiv

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

fun buildHttpClientJson(status: HttpStatusCode, response: Any): HttpClient {
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
                    jacksonObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(response),
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                    status = status
                )
            }
        }
    }
}
