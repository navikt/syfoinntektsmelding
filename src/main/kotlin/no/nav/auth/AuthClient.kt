package no.nav.auth

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
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
    MASKINPORTEN("maskinporten"),
    AZURE_AD("azuread"),
    IDPORTEN("idporten"),
    TOKEN_X("tokenx"),
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

data class TokenIntrospectionResponse(
    val active: Boolean,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val error: String?,
    @JsonAnySetter @get:JsonAnyGetter
    val other: Map<String, Any?> = mutableMapOf(),
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
            sikkerLogger().info("Requesting token for $target from ${provider.alias} and endpoint $tokenEndpoint")
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

    suspend fun exchange(
        provider: IdentityProvider,
        target: String,
        userToken: String,
    ): TokenResponse =
        try {
            httpClient
                .submitForm(
                    tokenExchangeEndpoint,
                    parameters {
                        set("target", target)
                        set("user_token", userToken)
                        set("identity_provider", provider.alias)
                    },
                ).body<TokenResponse.Success>()
        } catch (e: ResponseException) {
            TokenResponse.Error(e.response.body<TokenErrorResponse>(), e.response.status)
        }

    suspend fun introspect(
        provider: IdentityProvider,
        accessToken: String,
    ): TokenIntrospectionResponse =
        httpClient
            .submitForm(
                tokenIntrospectionEndpoint,
                parameters {
                    set("token", accessToken)
                    set("identity_provider", provider.alias)
                },
            ).body()
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
                        sikkerLogger().error("Feilet å hente token status: ${it.status} - ${it.error.errorDescription}")
                        throw RuntimeException("Feilet å hente token status: ${it.status} - ${it.error.errorDescription}")
                    }
                }
            }
        }
    }

fun AuthClient.fetchOboToken(
    target: String,
    userToken: String,
): () -> String =
    {
        runBlocking {
            exchange(IdentityProvider.TOKEN_X, target, userToken).let {
                when (it) {
                    is TokenResponse.Success -> it.accessToken
                    is TokenResponse.Error -> {
                        sikkerLogger().error("Feilet å hente obo token status: ${it.status} - ${it.error.errorDescription}")
                        throw RuntimeException("Feilet å hente obo token status: ${it.status} - ${it.error.errorDescription}")
                    }
                }
            }
        }
    }
