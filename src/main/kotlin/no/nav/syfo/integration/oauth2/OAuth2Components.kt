package no.nav.syfo.integration.oauth2

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import java.util.Optional
import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.client.core.context.JwtBearerTokenResolver
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.http.OAuth2HttpRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory

class DefaultOAuth2HttpClient(private val httpClient: HttpClient) : OAuth2HttpClient {
    val logger = LoggerFactory.getLogger(this::class.java)

    override fun post(oAuth2HttpRequest: OAuth2HttpRequest): OAuth2AccessTokenResponse {
        return runBlocking {
            try {
                return@runBlocking httpClient.submitForm(
                    url = oAuth2HttpRequest.tokenEndpointUrl.toString(),
                    formParameters = Parameters.build {
                        oAuth2HttpRequest.formParameters.forEach {
                            append(it.key, it.value)
                        }
                    }
                )
            } catch (ex: Exception) {
                if (ex is ClientRequestException) {
                    logger.error(ex.response.receive<String>())
                }

                throw ex
            }
        }
    }
}

class TokenResolver : JwtBearerTokenResolver {
    private var tokenPrincipal: TokenValidationContextPrincipal? = null

    override fun token(): Optional<String> {
        return tokenPrincipal?.context?.firstValidToken?.map { it.tokenAsString } ?: Optional.empty()
    }
}
