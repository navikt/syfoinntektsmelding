package no.nav.syfo.koin

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.ktor.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.OAuth2TokenProvider
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import no.nav.syfo.integration.oauth2.DefaultOAuth2HttpClient
import no.nav.syfo.integration.oauth2.OAuth2ClientPropertiesConfig
import no.nav.syfo.integration.oauth2.TokenResolver
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import java.net.URI

fun Module.externalSystemClients(config: ApplicationConfig) {

    single {
        val clientConfig = OAuth2ClientPropertiesConfig(config)
        val tokenResolver = TokenResolver()
        val oauthHttpClient = DefaultOAuth2HttpClient(get())
        val accessTokenService = OAuth2AccessTokenService(
            tokenResolver,
            OnBehalfOfTokenClient(oauthHttpClient),
            ClientCredentialsTokenClient(oauthHttpClient),
            TokenExchangeClient(oauthHttpClient)
        )

        val azureAdConfig = clientConfig.clientConfig["azure_ad"] ?: error("Fant ikke config i application.conf")
        OAuth2TokenProvider(accessTokenService, azureAdConfig)
    } bind AccessTokenProvider::class

    single (named("PROXY")){
        oauth2TokenProvider(
            config,
            config.getString("client.registration.clients[0].proxyscope")
        )
    } bind AccessTokenProvider::class

    single (named("OPPGAVE")){
        oauth2TokenProvider(
            config,
            config.getString("client.registration.clients[0].oppgavescope")
        )
    } bind AccessTokenProvider::class

    single (named("DOKARKIV")){
        oauth2TokenProvider(
            config,
            config.getString("client.registration.clients[0].dokarkivscope")
        )
    } bind AccessTokenProvider::class

    single (named("SAF")){
        oauth2TokenProvider(
            config,
            config.getString("client.registration.clients[0].safscope")
        )
    } bind AccessTokenProvider::class

    single (named("PDL")){
        oauth2TokenProvider(
            config,
            config.getString("client.registration.clients[0].pdlscope")
        )
    } bind AccessTokenProvider::class

}


private fun Scope.oauth2TokenProvider(config: ApplicationConfig, scope: String): OAuth2TokenProvider =
    OAuth2TokenProvider(
        oauth2Service = accessTokenService(this),
        clientProperties = config.azureAdConfig(scope)
    )

private fun accessTokenService(scope: Scope): OAuth2AccessTokenService =
    DefaultOAuth2HttpClient(scope.get()).let {
        OAuth2AccessTokenService(
            TokenResolver(),
            OnBehalfOfTokenClient(it),
            ClientCredentialsTokenClient(it),
            TokenExchangeClient(it)
        )
    }

private fun ApplicationConfig.azureAdConfig(scope: String): ClientProperties {
    val prefix = "client.registration.clients[0]"
    return ClientProperties(
        getString("$prefix.token_endpoint_url").let(::URI),
        getString("$prefix.well_known_url").let(::URI),
        getString("$prefix.grant_type").let(::OAuth2GrantType),
        scope.split(","),
        authProps(),
        null,
        null
    )
}

private fun ApplicationConfig.authProps(): ClientAuthenticationProperties {
    val prefix = "client.registration.clients[0].authentication"
    return ClientAuthenticationProperties(
        getString("$prefix.client_id"),
        getString("$prefix.client_auth_method").let(::ClientAuthenticationMethod),
        getString("$prefix.client_secret"),
        null
    )
}
