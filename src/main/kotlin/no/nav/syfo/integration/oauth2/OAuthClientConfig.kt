package no.nav.syfo.integration.oauth2

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.ktor.config.*
import io.ktor.util.*
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import java.net.URI

@KtorExperimentalAPI
class OAuth2ClientPropertiesConfig(
    applicationConfig: ApplicationConfig
) {
    internal val clientConfig: Map<String, ClientProperties> =
        applicationConfig.configList(CLIENTS_PATH)
            .associate { clientConfig ->
                val wellKnownUrl = clientConfig.propertyToStringOrNull("well_known_url")
                val resourceUrl = clientConfig.propertyToStringOrNull("resource_url")
                clientConfig.propertyToString(CLIENT_NAME) to ClientProperties(
                    URI(clientConfig.propertyToString("token_endpoint_url")),
                    wellKnownUrl?.let { URI(it) },
                    OAuth2GrantType(clientConfig.propertyToString("grant_type")),
                    clientConfig.propertyToStringOrNull("scope")?.split(","),
                    ClientAuthenticationProperties(
                        clientConfig.propertyToString("authentication.client_id"),
                        ClientAuthenticationMethod(
                            clientConfig.propertyToString("authentication.client_auth_method")
                        ),
                        clientConfig.propertyToStringOrNull("authentication.client_secret"),
                        clientConfig.propertyToStringOrNull("authentication.client_jwk")
                    ),
                    resourceUrl?.let { URI(it) },
                    ClientProperties.TokenExchangeProperties(
                        clientConfig.propertyToStringOrNull("token-exchange.audience") ?: "",
                        clientConfig.propertyToStringOrNull("token-exchange.resource")
                    )
                )
            }

    @KtorExperimentalAPI
    internal fun ApplicationConfig.propertyToString(prop: String) = this.property(prop).getString()

    @KtorExperimentalAPI
    internal fun ApplicationConfig.propertyToStringOrNull(prop: String) = this.propertyOrNull(prop)?.getString()


    companion object CommonConfigurationAttributes {
        private const val COMMON_PREFIX = "no.nav.security.jwt.client.registration"
        const val CLIENTS_PATH = "$COMMON_PREFIX.clients"
        const val CLIENT_NAME = "client_name"
    }
}
