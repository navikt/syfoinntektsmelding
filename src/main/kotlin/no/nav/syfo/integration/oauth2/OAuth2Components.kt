package no.nav.syfo.integration.oauth2

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.security.token.support.client.core.context.JwtBearerTokenResolver
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.core.http.OAuth2HttpRequest
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.util.Optional

class DefaultOAuth2HttpClient(private val httpClient: HttpClient) : OAuth2HttpClient {
    private val logger = this.logger()

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
                ).body()
            } catch (ex: Exception) {
                if (ex is ClientRequestException) {
                    logger.error(ex.response.body<String>())
                }

                throw ex
            }
        }
    }
}

class TokenResolver : JwtBearerTokenResolver {
    var tokenPrincipal: TokenValidationContextPrincipal? = null

    override fun token(): Optional<String> {
        return tokenPrincipal?.context?.firstValidToken?.map { it.tokenAsString } ?: Optional.empty()
    }
}
