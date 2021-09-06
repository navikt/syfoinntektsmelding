package no.nav.syfo.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.client.SakConsumer
import no.nav.syfo.client.azuread.AzureAdTokenConsumer
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.client.SakClient
import no.nav.syfo.client.TokenConsumer
import no.nav.syfo.client.aktor.AktorClient
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.service.BehandleInngaaendeJournalConsumer
import no.nav.syfo.service.InngaaendeJournalConsumer
import no.nav.syfo.service.JournalConsumer
import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.integration.kafka.*
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.*
import no.nav.syfo.client.saf.SafDokumentClient
import no.nav.syfo.client.saf.SafJournalpostClient
import no.nav.syfo.service.BehandlendeEnhetConsumer
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
        AktorClient(
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
            config.getString("kafka_utsatt_oppgave_topic")
        )
    }
    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get()) }
    single { IMStatsRepoImpl(get()) } bind IMStatsRepo::class
    single { DatapakkePublisherJob(get(), get(), config.getString("datapakke.api_url"), config.getString("datapakke.id")) }
    single { BehandlendeEnhetConsumer(get(), get(), get()) } bind BehandlendeEnhetConsumer::class
    single { UtsattOppgaveDAO(UtsattOppgaveRepositoryMockk()) }
    single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get(), get()) } bind OppgaveClient::class
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
    single { BehandleInngaaendeJournalConsumer(get()) } bind BehandleInngaaendeJournalConsumer::class
    single { JournalConsumer(get(), get(), get()) } bind JournalConsumer::class
    single { Metrikk() } bind Metrikk::class
    single { JournalpostService(get(), get(), get(), get(), get()) } bind JournalpostService::class
    single { EksisterendeSakService(get()) } bind EksisterendeSakService::class
    single { InntektsmeldingService(InntektsmeldingRepositoryImp(get()), get()) } bind InntektsmeldingService::class
    single { SakClient(config.getString("opprett_sak_url"), get(), get()) } bind SakClient::class
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
        SafJournalpostClient(
            get(),
            "http://localhost",
            get()
        )
    } bind SafJournalpostClient::class

    single {
        SafDokumentClient(
            config.getString("saf_dokument_url"),
            get(),
            get()
        )
    } bind SafDokumentClient::class

    single {
        DokArkivClient(
            config.getString("dokarkiv_url"),
            get(),
            get()
        )
    } bind DokArkivClient::class

    single {

        SakConsumer(
            get(),
            get(),
            config.getString("aad_syfoinntektsmelding_clientid_username"),
            config.getString("sakconsumer_host_url")
        )
    } bind SakConsumer::class
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
