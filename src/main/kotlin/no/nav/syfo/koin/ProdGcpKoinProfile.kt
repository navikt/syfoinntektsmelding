package no.nav.syfo.koin

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.OAuth2TokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.OAuth2GrantType
import no.nav.security.token.support.client.core.oauth2.ClientCredentialsTokenClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.core.oauth2.OnBehalfOfTokenClient
import no.nav.security.token.support.client.core.oauth2.TokenExchangeClient
import no.nav.syfo.MetrikkVarsler
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.norg.Norg2Client
import no.nav.syfo.client.saf.SafDokumentClient
import no.nav.syfo.client.saf.SafJournalpostClient
import no.nav.syfo.integration.kafka.UtsattOppgaveConsumer
import no.nav.syfo.integration.kafka.commonAivenProperties
import no.nav.syfo.integration.kafka.joarkAivenProperties
import no.nav.syfo.integration.kafka.journalpost.JournalpostHendelseConsumer
import no.nav.syfo.integration.kafka.utsattOppgaveAivenProperties
import no.nav.syfo.integration.oauth2.DefaultOAuth2HttpClient
import no.nav.syfo.integration.oauth2.TokenResolver
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.ArbeidsgiverperiodeRepository
import no.nav.syfo.repository.ArbeidsgiverperiodeRepositoryImp
import no.nav.syfo.repository.FeiletRepositoryImp
import no.nav.syfo.repository.FeiletService
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingRepositoryImp
import no.nav.syfo.repository.UtsattOppgaveRepositoryImp
import no.nav.syfo.service.BehandleInngaaendeJournalConsumer
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.service.InngaaendeJournalConsumer
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.service.JournalConsumer
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.simba.InntektsmeldingConsumer
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URI
import javax.sql.DataSource

fun prodConfig(config: ApplicationConfig) = module {

    val clientConfig = config.configList("no.nav.security.jwt.client.registration.clients").first()
    single(named("PROXY")) {

        oauth2TokenProvider(
            clientConfig,
            clientConfig.getString("proxyscope")
        )
    } bind AccessTokenProvider::class

    single(named("OPPGAVE")) {
        oauth2TokenProvider(
            clientConfig,
            clientConfig.getString("oppgavescope")
        )
    } bind AccessTokenProvider::class

    single(named("DOKARKIV")) {
        oauth2TokenProvider(
            clientConfig,
            clientConfig.getString("dokarkivscope")
        )
    } bind AccessTokenProvider::class

    single(named("SAF")) {
        oauth2TokenProvider(
            clientConfig,
            clientConfig.getString("safscope")
        )
    } bind AccessTokenProvider::class

    single(named("PDL")) {
        oauth2TokenProvider(
            clientConfig,
            clientConfig.getString("pdlscope")
        )
    } bind AccessTokenProvider::class

    externalSystemClients(config)
    single {
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.getjdbcUrlFromProperties()
                username = config.getString("database.username")
                password = config.getString("database.password")
                maximumPoolSize = 5
                minimumIdle = 1
                idleTimeout = 10001
                connectionTimeout = 2000
                maxLifetime = 30001
                driverClassName = "org.postgresql.Driver"
            }
        )
    } bind DataSource::class

    single {
        JoarkInntektsmeldingHendelseProsessor(
            get(), get(), get(), get(), get()
        )
    } bind JoarkInntektsmeldingHendelseProsessor::class

    single {
        InntektsmeldingBehandler(
            get(), get(), get(), get(), get(), get()
        )
    } bind InntektsmeldingBehandler::class

    single { InngaaendeJournalConsumer(get()) } bind InngaaendeJournalConsumer::class
    single { BehandleInngaaendeJournalConsumer(get()) } bind BehandleInngaaendeJournalConsumer::class
    single { JournalConsumer(get(), get(), get()) } bind JournalConsumer::class
    single { Metrikk() } bind Metrikk::class
    single { BehandlendeEnhetConsumer(get(), get(), get()) } bind BehandlendeEnhetConsumer::class
    single { JournalpostService(get(), get(), get(), get(), get()) } bind JournalpostService::class
    single { InntektsmeldingRepositoryImp(get()) } bind InntektsmeldingRepository::class
    single { InntektsmeldingService(get(), get()) } bind InntektsmeldingService::class
    single { ArbeidsgiverperiodeRepositoryImp(get()) } bind ArbeidsgiverperiodeRepository::class

    single {
        JournalpostHendelseConsumer(
            joarkAivenProperties(), config.getString("kafka_joark_hendelse_topic"), get(), get()
        )
    }
    single {
        UtsattOppgaveConsumer(
            utsattOppgaveAivenProperties(), config.getString("kafka_utsatt_oppgave_topic"), get(), get(), get()
        )
    }

    single {
        InntektsmeldingAivenProducer(
            commonAivenProperties()
        )
    }

    single { UtsattOppgaveDAO(UtsattOppgaveRepositoryImp(get())) }
    single {
        val tokenProvider = get<AccessTokenProvider>(qualifier = named("OPPGAVE"))
        OppgaveClient(config.getString("oppgavebehandling_url"), get(), get(), tokenProvider::getToken)
    } bind OppgaveClient::class
    single { UtsattOppgaveService(get(), get(), get(), get(), get(), get()) } bind UtsattOppgaveService::class
    single { FeiletUtsattOppgaveMeldingProsessor(get(), get()) }

    single {
        FjernInntektsmeldingByBehandletProcessor(
            InntektsmeldingRepositoryImp(get()), config.getString("lagringstidMåneder").toInt()
        )
    } bind FjernInntektsmeldingByBehandletProcessor::class
    single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get(), get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class

    single { FeiletService(FeiletRepositoryImp(get())) } bind FeiletService::class

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get(), bakgrunnsvarsler = MetrikkVarsler()) }

    single {
        PdlClientImpl(
            config.getString("pdl_url"),
            get(qualifier = named("PDL")),
            get(),
            get()
        )
    } bind PdlClient::class

    single {
        Norg2Client(
            config.getString("norg2_url"),
            get(),
            get<AccessTokenProvider>(qualifier = named("PROXY"))::getToken,
        )
    } bind Norg2Client::class

    single {
        SafJournalpostClient(
            get(),
            config.getString("saf_journal_url"),
            get(qualifier = named("SAF")),
        )
    } bind SafJournalpostClient::class

    single {
        SafDokumentClient(
            config.getString("saf_dokument_url"),
            get(),
            get(qualifier = named("SAF")),
        )
    } bind SafDokumentClient::class

    single {
        DokArkivClient(
            config.getString("dokarkiv_url"),
            get(qualifier = named("DOKARKIV")),
            get()
        )
    } bind DokArkivClient::class

// TODO: trekk ut topic og consumerConfig-properties
    single {
        InntektsmeldingConsumer(
            commonAivenProperties() + mapOf(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.CLIENT_ID_CONFIG to "syfoinntektsmelding-im-consumer",
                ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-im-v1",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
            ),
            "helsearbeidsgiver.inntektsmelding",
            get(),
            get(),
            get(),
            get()
        )
    }
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
    return ClientProperties(
        getString("token_endpoint_url").let(::URI),
        getString("well_known_url").let(::URI),
        getString("grant_type").let(::OAuth2GrantType),
        scope.split(","),
        authProps(),
        null,
        null
    )
}

private fun ApplicationConfig.authProps(): ClientAuthenticationProperties {
    val prefix = "authentication"
    return ClientAuthenticationProperties(
        getString("$prefix.client_id"),
        getString("$prefix.client_auth_method").let(::ClientAuthenticationMethod),
        getString("$prefix.client_secret"),
        null
    )
}
