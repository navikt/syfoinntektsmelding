package no.nav.syfo.koin

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import io.ktor.server.config.ApplicationConfig
import no.nav.auth.AuthClient
import no.nav.auth.IdentityProvider
import no.nav.auth.fetchToken
import no.nav.helsearbeidsgiver.oppgave.OppgaveClient
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
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.norg.Norg2Client
import no.nav.syfo.client.saf.SafDokumentClient
import no.nav.syfo.client.saf.SafJournalpostClient
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
    single {
        AuthClient(
            httpClient = get(),
            tokenEndpoint = config.getString("auth.token_endpoint"),
            tokenExchangeEndpoint = config.getString("auth.token_exchange_endpoint"),
            tokenIntrospectionEndpoint = config.getString("auth.token_introspection_endpoint"),
        )
    }

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
        PdlClient(
            config.getString("pdl_url"),
            Behandlingsgrunnlag.INNTEKTSMELDING,
            get<AccessTokenProvider>(qualifier = named(AccessScope.PDL))::getToken,
        )
    } bind PdlClient::class
    single {
        val azureClient: AuthClient = get()
        OppgaveClient(
            url = config.getString("oppgavebehandling_url"),
            getToken = azureClient.fetchToken(IdentityProvider.AZURE_AD, config.getString("oppgavescope")),
        )
    }
    single {
        Norg2Client(
            url = config.getString("norg2_url"),
            httpClient = get(),
        )
    }

    single {
        val azureClient: AuthClient = get()
        SafJournalpostClient(
            httpClient = get(),
            basePath = config.getString("saf_journal_url"),
            getAccessToken = azureClient.fetchToken(IdentityProvider.AZURE_AD, config.getString("safscope")),
        )
    }

    single {
        val azureClient: AuthClient = get()
        SafDokumentClient(
            url = config.getString("saf_dokument_url"),
            httpClient = get(),
            getAccessToken = azureClient.fetchToken(IdentityProvider.AZURE_AD, config.getString("safscope")),
        )
    }

    single {
        val azureClient: AuthClient = get()
        DokArkivClient(
            url = config.getString("dokarkiv_url"),
            httpClient = get(),
            getAccessToken = azureClient.fetchToken(IdentityProvider.AZURE_AD, config.getString("docarkivscope")),
        )
    }
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
