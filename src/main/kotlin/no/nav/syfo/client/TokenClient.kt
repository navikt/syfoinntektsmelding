package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandling.TokenException
import java.util.Base64

class TokenConsumer(
    private val httpClient: HttpClient,
    private val url: String,
    private val username: String,
    private val password: String
) {
    val token: String
        get() {
            var result = ""
            val genUrl = "$url?grant_type=client_credentials&scope=openid"
            runBlocking {
                try {
                    result = httpClient.get<Token>(genUrl) {
                        header(HttpHeaders.Authorization, "Basic " + "$username:$password".toBase64())
                    }.access_token
                } catch (cause: ClientRequestException) {
                    throw TokenException(cause.response?.status?.value, cause)
                }
            }
            return result
        }
}

fun String.toBase64(): String = Base64.getEncoder().encodeToString(this.toByteArray())

data class Token(val access_token: String, val token_type: String, var expires_in: Int = 0)
