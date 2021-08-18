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
import no.nav.syfo.consumer.rest.norg.Norg2Client

import no.nav.syfo.consumer.ws.*
import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.integration.kafka.*
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.*
import no.nav.syfo.saf.SafDokumentClient
import no.nav.syfo.saf.SafJournalpostClient
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService

import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

@KtorExperimentalAPI
fun preprodConfig(config: ApplicationConfig) = module {
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
    single { ArbeidsgiverperiodeRepositoryImp(get())} bind ArbeidsgiverperiodeRepository::class

    single {
        AktorConsumer(
            get(),
            config.getString("srvsyfoinntektsmelding.username"),
            config.getString("aktoerregister_api_v1_url"),
            get()
        )
    } bind AktorConsumer::class
    single {
        TokenConsumer(
            get(),
            config.getString("security-token-service-token.url"),
            config.getString("srvsyfoinntektsmelding.username"),
            config.getString("srvsyfoinntektsmelding.password")
        )
    } bind TokenConsumer::class
    single {
        InntektsmeldingBehandler(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind InntektsmeldingBehandler::class

    single { InngaaendeJournalConsumer(get()) } bind InngaaendeJournalConsumer::class

    single { Metrikk() } bind Metrikk::class
    single { BehandlendeEnhetConsumer(get(), get(), get()) } bind BehandlendeEnhetConsumer::class
    single { JournalpostService(get(), get(), get(), get(), get(), get()) } bind JournalpostService::class
    single { DatapakkePublisherJob(get(), get(), config.getString("datapakke.api_url"), config.getString("datapakke.id")) }

    single {
        AzureAdTokenConsumer(
            get(StringQualifier("proxyHttpClient")),
            config.getString("aadaccesstoken_url"),
            config.getString("aad_syfoinntektsmelding_clientid_username"),
            config.getString("aad_syfoinntektsmelding_clientid_password")
        )
    } bind AzureAdTokenConsumer::class

    single {
        SakConsumer(
            get(),
            get(),
            config.getString("aad_syfogsak_clientid_username"),
            config.getString("sakconsumer_host_url")
        )
    } bind SakConsumer::class

    single { InntektsmeldingRepositoryImp(get()) } bind InntektsmeldingRepository::class
    single { EksisterendeSakService(get()) } bind EksisterendeSakService::class
    single { InntektsmeldingService( get(), get()) } bind InntektsmeldingService::class
    single { SakClient(config.getString("opprett_sak_url"), get()) } bind SakClient::class
    single { SaksbehandlingService(get(), get(), get(), get()) } bind SaksbehandlingService::class

    single {
        JoarkHendelseKafkaClient(
            joarkOnPremProperties(config).toMutableMap(),
            config.getString("kafka_joark_hendelse_topic")
        )
    }
    single {
        UtsattOppgaveKafkaClient(
            utsattOppgaveOnPremProperties(config).toMutableMap(),
            config.getString("kafka_utsatt_oppgave_topic"), get(), get(), get()
        )
    }

    single {
        InntektsmeldingAivenProducer(
            producerAivenProperties(config)
        )
    }

    single { UtsattOppgaveDAO(UtsattOppgaveRepositoryImp(get())) }
    single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get()) } bind OppgaveClient::class
    single { UtsattOppgaveService(get(), get(), get()) } bind UtsattOppgaveService::class
    single { FeiletUtsattOppgaveMeldingProsessor(get(), get()) }

    single {
        FjernInntektsmeldingByBehandletProcessor(
            InntektsmeldingRepositoryImp(get()),
            config.getString("lagringstidMåneder").toInt()
        )
    } bind FjernInntektsmeldingByBehandletProcessor::class
    single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class

    single { FeiletService(FeiletRepositoryImp(get())) } bind FeiletService::class

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get(), bakgrunnsvarsler = MetrikkVarsler()) }
    single { IMStatsRepoImpl(get()) } bind IMStatsRepo::class

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
        Norg2Client(
            config.getString("norg2_url"),
            RestSTSAccessTokenProvider(
                config.getString("security_token.username"),
                config.getString("security_token.password"),
                config.getString("security_token_service_token_url"),
                get()
            ),
            get()
        )
    } bind Norg2Client::class

    single {
        SafJournalpostClient(
            get(),
            config.getString("saf_journal_url"),
            config.getString("")


        )
    } bind SafJournalpostClient::class

    single {
        SafDokumentClient(
            config.getString("saf_dokument_url"),
            get()
        )
    } bind SafDokumentClient::class

}

