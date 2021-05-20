package no.nav.syfo.consumer.azuread

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandling.AzureAdTokenException


class AzureAdTokenConsumer(private val httpClient: HttpClient,
                           private val url: String,
                           private val clientId: String,
                           private val clientSecret: String) {

    fun getAccessToken(resource: String): String? {
        var token: AzureAdToken? = null
        runBlocking {
            try {
                token = httpClient.submitForm<AzureAdToken>(
                    url = url,
                    formParameters = Parameters.build {
                        append("client_id", clientId)
                        append("resource", resource)
                        append("grant_type", "client_credentials")
                        append("client_secret", clientSecret)
                    })
            } catch (cause: ClientRequestException) {
                throw AzureAdTokenException(cause.response?.status?.value, cause)
            }
        }
        return token?.access_token
    }
}

data class AzureAdToken(
        val access_token: String? = null,
        val token_type: String? = null,
        val expires_in: String? = null,
        val ext_expires_in: String? = null,
        val expires_on: String? = null,
        val not_before: String? = null,
        val resource: String? = null
)
