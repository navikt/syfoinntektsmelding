package no.nav.syfo.koin

import com.zaxxer.hikari.HikariConfig
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.RestSTSAccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.MetrikkVarsler
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.config.OppgaveClientConfigProvider
import no.nav.syfo.config.SakClientConfigProvider
import no.nav.syfo.consumer.SakConsumer
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.rest.TokenConsumer
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.util.ws.createServicePort
import no.nav.syfo.consumer.ws.*
import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.integration.kafka.*
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.*
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.BehandleInngaaendeJournalV1
import no.nav.tjeneste.virksomhet.behandlesak.v2.BehandleSakV2
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.InngaaendeJournalV1
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.binding.OppgavebehandlingV3
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    externalSystemClients(config)
    single {
        val vaultconfig = HikariConfig()
        vaultconfig.jdbcUrl = config.getjdbcUrlFromProperties()
        vaultconfig.minimumIdle = 1
        vaultconfig.maximumPoolSize = 2
        HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            vaultconfig,
            config.getString("database.vault.mountpath"),
            config.getString("database.vault.admin"),
        )
    } bind DataSource::class

    single {
        OppgaveClientConfigProvider(
            config.getString("oppgavebehandling_url"),
            config.getString("security_token_service_token_url"),
            config.getString("service_user.username"),
            config.getString("service_user.password")
        )
    }
    single {
        SakClientConfigProvider(
            config.getString("opprett_sak_url"),
            config.getString("security_token_service_token_url"),
            config.getString("service_user.username"),
            config.getString("service_user.password")
        )
    }
    single {
        JoarkInntektsmeldingHendelseProsessor(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind JoarkInntektsmeldingHendelseProsessor::class

    single { AktorConsumer(get(), config.getString("srvsyfoinntektsmelding.username"), config.getString("aktoerregister_api_v1_url"), get()) } bind AktorConsumer::class
    single {
        TokenConsumer(
            get(),
            config.getString("security-token-service-token.url"),
            config.getString("srvsyfoinntektsmelding.username"),
            config.getString("srvsyfoinntektsmelding.password")
        )
    } bind TokenConsumer::class
    single { InntektsmeldingBehandler(get(), get(),get(), get(), get(), get(), get()) } bind InntektsmeldingBehandler::class


    single { InngaaendeJournalConsumer(get()) } bind InngaaendeJournalConsumer::class
    single { BehandleInngaaendeJournalConsumer(get()) } bind BehandleInngaaendeJournalConsumer::class
    single { JournalConsumer(get(), get()) } bind JournalConsumer::class
    single { Metrikk() } bind Metrikk::class
    single { BehandlendeEnhetConsumer(get(), get(), get()) } bind BehandlendeEnhetConsumer::class
    single { JournalpostService(get(), get(), get(), get(), get()) } bind JournalpostService::class

    single { AzureAdTokenConsumer(
        get(StringQualifier("proxyHttpClient")),
        config.getString("aadaccesstoken_url"),
        config.getString("aad_syfoinntektsmelding_clientid_username"),
        config.getString("aad_syfoinntektsmelding_clientid_password")) } bind AzureAdTokenConsumer::class

    single { SakConsumer(get(),
        get(),
        config.getString("aad_syfogsak_clientid_username"),
        config.getString("sakconsumer_host_url"))} bind SakConsumer::class

    single { EksisterendeSakService(get()) } bind EksisterendeSakService::class
    single { InntektsmeldingRepositoryImp(get()) } bind InntektsmeldingRepository::class
    single { InntektsmeldingService(get(),get()) } bind InntektsmeldingService::class
    single { ArbeidsgiverperiodeRepositoryImp(get())} bind ArbeidsgiverperiodeRepository::class
    single { SakClient(config.getString("opprett_sak_url"), get()) } bind SakClient::class
    single { SaksbehandlingService(get(), get(), get(), get()) } bind SaksbehandlingService::class
    single { DatapakkePublisherJob(get(), get(), config.getString("datapakke.api_url"), config.getString("datapakke.id"), false) }

    single { JoarkHendelseKafkaClient(
        joarkOnPremProperties(config).toMutableMap(),
        config.getString("kafka_joark_hendelse_topic")
    ) }
    single {
        UtsattOppgaveKafkaClient(
            utsattOppgaveOnPremProperties(config).toMutableMap(),
            config.getString("kafka_utsatt_oppgave_topic"), get(), get(), get()
        )
    }

    single { InntektsmeldingAivenProducer(producerAivenProperties(config)) }

    single { UtsattOppgaveDAO(UtsattOppgaveRepositoryImp(get()))}
    single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get())} bind OppgaveClient::class
    single { UtsattOppgaveService(get(), get(), get()) } bind UtsattOppgaveService::class
    single { FeiletUtsattOppgaveMeldingProsessor(get(), get() ) }

    single { FjernInntektsmeldingByBehandletProcessor(InntektsmeldingRepositoryImp(get()), config.getString("lagringstidMåneder").toInt() )} bind FjernInntektsmeldingByBehandletProcessor::class
    single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class

    single { OppgavebehandlingConsumer(get()) } bind OppgavebehandlingConsumer::class

    single { FeiletService(FeiletRepositoryImp(get())) } bind FeiletService::class

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { IMStatsRepoImpl(get()) } bind IMStatsRepo::class
    single { BakgrunnsjobbService(get(), bakgrunnsvarsler = MetrikkVarsler()) }

    single {
        PdlClientImpl(
            config.getString("pdl_url"),
            RestSTSAccessTokenProvider(
                config.getString("security_token.username"),
                config.getString("security_token.password"),
                config.getString("security_token_service_token_url"),
                get()
            ),
            get(),
            get()
        )
    } bind PdlClient::class

    single {
        createServicePort(
            serviceUrl = config.getString("virksomhet_arbeidsfordeling_v1_endpointurl"),
            serviceClazz = ArbeidsfordelingV1::class.java
        )
    } bind ArbeidsfordelingV1::class

    single {
        createServicePort(
            serviceUrl = config.getString("servicegateway_url"),
            serviceClazz = OppgavebehandlingV3::class.java
        )

    } bind OppgavebehandlingV3::class

    single {
        createServicePort(
            serviceUrl = config.getString("journal_v2_endpointurl"),
            serviceClazz = JournalV2::class.java
        )
    } bind JournalV2::class

    single {
        createServicePort(
            serviceUrl = config.getString("inngaaendejournal_v1_endpointurl"),
            serviceClazz = InngaaendeJournalV1::class.java
        )
    } bind InngaaendeJournalV1::class

    single {
        createServicePort(
            serviceUrl = config.getString("virksomhet_behandlesak_v2_endpointurl"),
            serviceClazz = BehandleSakV2::class.java
        )
    } bind BehandleSakV2::class

    single {
        createServicePort(
            serviceUrl = config.getString("behandleinngaaendejournal_v1_endpointurl"),
            serviceClazz = BehandleInngaaendeJournalV1::class.java
        )
    } bind BehandleInngaaendeJournalV1::class
}
