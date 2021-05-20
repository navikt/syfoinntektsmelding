package no.nav.syfo.consumer.rest


import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandling.TokenException
import java.util.*

class TokenConsumer(
    private val httpClient: HttpClient,
    private val url: String,
    private val username: String,
    private val password: String
    ) {
    val token: String
        get() {
            var result = ""
            val params = ParametersBuilder()
            params.append("grant_type", "client_credentials")
            params.append("scope", "openid")
            val genUrl = URLBuilder(URLProtocol.HTTPS, url, parameters = params).toString()
            runBlocking {
                try {
                    result = httpClient.get<Token>(genUrl){
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
