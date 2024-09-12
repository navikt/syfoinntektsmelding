package no.nav.syfo.koin

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.ktor.server.config.ApplicationConfig
import no.nav.helsearbeidsgiver.pdl.Behandlingsgrunnlag
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.tokenprovider.AccessTokenProvider
import no.nav.helsearbeidsgiver.tokenprovider.DefaultOAuth2HttpClient
import no.nav.helsearbeidsgiver.tokenprovider.OAuth2TokenProvider
import no.nav.helsearbeidsgiver.tokenprovider.TokenResolver
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import no.nav.syfo.util.getString
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.QualifierValue
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import java.net.URI

enum class AccessScope : Qualifier {
    DOKARKIV,
    OPPGAVE,
    PDL,
    SAF,
    ;

    override val value: QualifierValue
        get() = name
}

fun Module.externalSystemClients(config: ApplicationConfig) {
    val clientConfig = config.configList("no.nav.security.jwt.client.registration.clients").first()

    single(named(AccessScope.OPPGAVE)) {
        oauth2TokenProvider(
            clientConfig,
            clientConfig.getString("oppgavescope"),
        )
    } bind AccessTokenProvider::class

    single(named(AccessScope.DOKARKIV)) {
        oauth2TokenProvider(
            clientConfig,
            clientConfig.getString("dokarkivscope"),
        )
    } bind AccessTokenProvider::class

    single(named(AccessScope.SAF)) {
        oauth2TokenProvider(
            clientConfig,
            clientConfig.getString("safscope"),
        )
    } bind AccessTokenProvider::class

    single(named(AccessScope.PDL)) {
        oauth2TokenProvider(
            clientConfig,
            clientConfig.getString("pdlscope"),
        )
    } bind AccessTokenProvider::class
    single {
        val tokenProvider: AccessTokenProvider = get(qualifier = named("PDL"))
        PdlClient(config.getString("pdl_url"), Behandlingsgrunnlag.INNTEKTSMELDING, tokenProvider::getToken)
    } bind PdlClient::class
}

private fun Scope.oauth2TokenProvider(
    config: ApplicationConfig,
    scope: String,
): OAuth2TokenProvider =
    OAuth2TokenProvider(
        oauth2Service = accessTokenService(),
        clientProperties = config.azureAdConfig(scope),
    )

private fun accessTokenService(): OAuth2AccessTokenService =
    DefaultOAuth2HttpClient().let {
        OAuth2AccessTokenService(
            TokenResolver(),
            OnBehalfOfTokenClient(it),
            ClientCredentialsTokenClient(it),
            TokenExchangeClient(it),
        )
    }

private fun ApplicationConfig.azureAdConfig(scope: String): ClientProperties =
    ClientProperties(
        getString("token_endpoint_url").let(::URI),
        getString("well_known_url").let(::URI),
        getString("grant_type").let(::OAuth2GrantType),
        scope.split(","),
        authProps(),
        null,
        null,
    )

private fun ApplicationConfig.authProps(): ClientAuthenticationProperties {
    val prefix = "authentication"
    return ClientAuthenticationProperties(
        getString("$prefix.client_id"),
        getString("$prefix.client_auth_method").let(::ClientAuthenticationMethod),
        getString("$prefix.client_secret"),
        null,
    )
}
