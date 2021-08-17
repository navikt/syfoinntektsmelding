package no.nav.syfo.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.config.OppgaveClientConfigProvider
import no.nav.syfo.config.SakClientConfigProvider
import no.nav.syfo.consumer.SakConsumer
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.rest.TokenConsumer
import no.nav.syfo.consumer.rest.aktor.AktorConsumer

import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer

import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.integration.kafka.*
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.*
import no.nav.syfo.saf.SafJournalpostClient
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService

import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

@KtorExperimentalAPI
fun localDevConfig(config: ApplicationConfig) = module {
    mockExternalDependecies()

    single {
        AktorConsumer(
            get(),
            config.getString("srvsyfoinntektsmelding.username"),
            config.getString("aktoerregister_api_v1_url"),
            get()
        )
    }
    single {
        TokenConsumer(
            get(),
            config.getString("security-token-service-token.url"),
            config.getString("srvsyfoinntektsmelding.username"),
            config.getString("srvsyfoinntektsmelding.password")
        )
    }
    single { InntektsmeldingRepositoryMock() } bind InntektsmeldingRepository::class

    single {
        HikariDataSource(
            createHikariConfig(
                config.getjdbcUrlFromProperties(),
                config.getString("database.username"),
                config.getString("database.password")
            )
        )
    } bind DataSource::class
    single { FeiletRepositoryImp(get()) } bind FeiletRepository::class
    single { UtsattOppgaveRepositoryImp(get()) } bind UtsattOppgaveRepository::class

    single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class

    single { JoarkHendelseKafkaClient(
         joarkLocalProperties().toMutableMap(),
        config.getString("kafka_joark_hendelse_topic")
    ) }
    single {
        UtsattOppgaveKafkaClient(
            utsattOppgaveLocalProperties().toMutableMap(),
            config.getString("kafka_utsatt_oppgave_topic"), get(), get(), get()
        )
    }
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

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get()) }
    single { IMStatsRepoImpl(get()) } bind IMStatsRepo::class
    single { DatapakkePublisherJob(get(), get(), config.getString("datapakke.api_url"), config.getString("datapakke.id")) }


    single { BehandlendeEnhetConsumer(get(), get(), get()) } bind BehandlendeEnhetConsumer::class
    single { UtsattOppgaveDAO(UtsattOppgaveRepositoryMockk()) }
    single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get()) } bind OppgaveClient::class
    single { UtsattOppgaveService(get(), get(), get()) } bind UtsattOppgaveService::class
    single { FeiletUtsattOppgaveMeldingProsessor(get(), get()) }

    single { FjernInntektsmeldingByBehandletProcessor(get(), 1) } bind FjernInntektsmeldingByBehandletProcessor::class

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
    single { JournalpostService(get(), get(), get(), get(), get(), get()) } bind JournalpostService::class
    single { EksisterendeSakService(get()) } bind EksisterendeSakService::class
    single { InntektsmeldingService(InntektsmeldingRepositoryImp(get()), get()) } bind InntektsmeldingService::class
    single { SakClient(config.getString("opprett_sak_url"), get()) } bind SakClient::class
    single { SaksbehandlingService(get(), get(), get(), get()) } bind SaksbehandlingService::class
    single { FeiletService(get()) } bind FeiletService::class
    single {
        JoarkInntektsmeldingHendelseProsessor(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind JoarkInntektsmeldingHendelseProsessor::class

    single {
        InntektsmeldingAivenProducer(producerLocalProperties(config.getString("kafka_bootstrap_servers")))
    }

    single {
        SakConsumer(
            get(),
            get(),
            config.getString("aad_syfoinntektsmelding_clientid_username"),
            config.getString("sakconsumer_host_url")
        )
    } bind SakConsumer::class

    single {
        SafJournalpostClient(
            get(),
            config.getString(""),
            config.getString("")
        )
    } bind SafJournalpostClient::class

    single {
        AzureAdTokenConsumer(
            get(),
            config.getString("aadaccesstoken_url"),
            config.getString("aad_syfogsak_clientid_username"),
            config.getString("aad_syfoinntektsmelding_clientid_password")
        )
    } bind AzureAdTokenConsumer::class
    single { ArbeidsgiverperiodeRepositoryImp(get())} bind ArbeidsgiverperiodeRepository::class
}
