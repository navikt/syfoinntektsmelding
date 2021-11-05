package no.nav.syfo.koin

import io.ktor.config.*
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.OAuth2TokenProvider
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import no.nav.syfo.integration.oauth2.DefaultOAuth2HttpClient
import no.nav.syfo.integration.oauth2.OAuth2ClientPropertiesConfig
import no.nav.syfo.integration.oauth2.TokenResolver
import org.koin.core.module.Module
import org.koin.dsl.bind

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
}
