package no.nav.syfo.util

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson

internal fun createHttpClient(maxRetries: Int): HttpClient = HttpClient(Apache5) { configure(maxRetries) }

internal fun HttpClientConfig<*>.configure(retries: Int) {
    expectSuccess = true

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(customObjectMapper()))
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    install(HttpRequestRetry) {
        retryOnException(
            maxRetries = retries,
            retryOnTimeout = true,
        )
        exponentialDelay()
    }

    install(HttpTimeout) {
        connectTimeoutMillis = 1000
        requestTimeoutMillis = 1000
        socketTimeoutMillis = 1000
    }
}

internal fun HttpRequestBuilder.navCallId(callId: String) {
    header("Nav-Call-Id", callId)
}
