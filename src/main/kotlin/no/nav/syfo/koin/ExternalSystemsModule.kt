package no.nav.syfo.koin

import io.ktor.config.*
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.OAuth2TokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.altinn.AltinnRestClient
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.helse.arbeidsgiver.web.auth.AltinnOrganisationsRepository
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import no.nav.syfo.integration.kafka.JoarchhendlseConsumerFactory
import no.nav.syfo.integration.kafka.UtsattOppgaveConsumerFactory
import no.nav.syfo.integration.kafka.UtsattOppgaveKafkaConsumer
import no.nav.syfo.integration.kafka.onPremCommonKafkaProps
import no.nav.syfo.kafkamottak.JoarkHendelseConsumer
import org.koin.core.module.Module
import org.koin.dsl.bind
import no.nav.syfo.integration.altinn.CachedAuthRepo
import no.nav.syfo.integration.oauth2.DefaultOAuth2HttpClient
import no.nav.syfo.integration.oauth2.OAuth2ClientPropertiesConfig
import no.nav.syfo.integration.oauth2.TokenResolver

fun Module.externalSystemClients(config: ApplicationConfig) {
    single {
        CachedAuthRepo(
            AltinnRestClient(
                config.getString("altinn.service_owner_api_url"),
                config.getString("altinn.gw_api_key"),
                config.getString("altinn.altinn_api_key"),
                config.getString("altinn.service_id"),
                get()
            )
        )
    } bind AltinnOrganisationsRepository::class

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



    single { UtsattOppgaveKafkaConsumer(
        onPremCommonKafkaProps(config),
        config.getString("kafka_utsatt_oppgave_topic"),
        get(),
        UtsattOppgaveConsumerFactory(),get(), get()) }
    single { JoarkHendelseConsumer(
        onPremCommonKafkaProps(config),
        config.getString("kafka_joark_hendelse_topics"),
        get(),
        JoarchhendlseConsumerFactory(config.getString("kafka_schema_registry_url_config")), get()) }




}
