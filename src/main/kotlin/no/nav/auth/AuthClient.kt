package no.nav.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.forms.submitForm
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

enum class IdentityProvider(
    @JsonValue val alias: String,
) {
    AZURE_AD("azuread"),
}

sealed class TokenResponse {
    data class Success(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("expires_in")
        val expiresInSeconds: Int,
    ) : TokenResponse()

    data class Error(
        val error: TokenErrorResponse,
        val status: HttpStatusCode,
    ) : TokenResponse()
}

data class TokenErrorResponse(
    val error: String,
    @JsonProperty("error_description")
    val errorDescription: String,
)

class AuthClient(
    private val httpClient: HttpClient,
    private val tokenEndpoint: String,
    private val tokenExchangeEndpoint: String,
    private val tokenIntrospectionEndpoint: String,
) {
    suspend fun token(
        provider: IdentityProvider,
        target: String,
    ): TokenResponse =
        try {
            sikkerLogger().info("Forsøker å hente token for $target fra ${provider.alias} og endpoint $tokenEndpoint")
            httpClient
                .submitForm(
                    tokenEndpoint,
                    parameters {
                        set("target", target)
                        set("identity_provider", provider.alias)
                    },
                ).body<TokenResponse.Success>()
                .apply { sikkerLogger().info("Hentet token expiresInSeconds: $expiresInSeconds") }
        } catch (e: ResponseException) {
            TokenResponse.Error(e.response.body<TokenErrorResponse>(), e.response.status)
        }
}

fun AuthClient.fetchToken(
    identityProvider: IdentityProvider,
    target: String,
): () -> String =
    {
        runBlocking {
            token(identityProvider, target).let {
                when (it) {
                    is TokenResponse.Success -> it.accessToken
                    is TokenResponse.Error -> {
                        sikkerLogger().error("Feilet å hente token. status: ${it.status} - ${it.error.errorDescription}")
                        throw RuntimeException("Feilet å hente token. status: ${it.status} - ${it.error.errorDescription}")
                    }
                }
            }
        }
    }
