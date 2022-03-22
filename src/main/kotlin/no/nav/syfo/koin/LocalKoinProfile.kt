package no.nav.syfo.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.client.BrregClient
import no.nav.syfo.client.MockBrregClient
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.client.SakClient
import no.nav.syfo.client.TokenConsumer
import no.nav.syfo.client.aktor.AktorClient
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.saf.SafDokumentClient
import no.nav.syfo.client.saf.SafJournalpostClient
import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.integration.kafka.JoarkHendelseKafkaClient
import no.nav.syfo.integration.kafka.UtsattOppgaveKafkaClient
import no.nav.syfo.integration.kafka.joarkLocalProperties
import no.nav.syfo.integration.kafka.producerLocalProperties
import no.nav.syfo.integration.kafka.utsattOppgaveLocalProperties
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.ArbeidsgiverperiodeRepository
import no.nav.syfo.repository.ArbeidsgiverperiodeRepositoryImp
import no.nav.syfo.repository.DuplikatRepository
import no.nav.syfo.repository.DuplikatRepositoryImpl
import no.nav.syfo.repository.FeiletRepository
import no.nav.syfo.repository.FeiletRepositoryImp
import no.nav.syfo.repository.FeiletService
import no.nav.syfo.repository.IMStatsRepo
import no.nav.syfo.repository.IMStatsRepoImpl
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingRepositoryImp
import no.nav.syfo.repository.InntektsmeldingRepositoryMock
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.repository.UtsattOppgaveRepository
import no.nav.syfo.repository.UtsattOppgaveRepositoryImp
import no.nav.syfo.repository.UtsattOppgaveRepositoryMockk
import no.nav.syfo.repository.createHikariConfig
import no.nav.syfo.service.BehandleInngaaendeJournalConsumer
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.service.InngaaendeJournalConsumer
import no.nav.syfo.service.JournalConsumer
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

    single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get(), get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class

    single {
        JoarkHendelseKafkaClient(
            joarkLocalProperties().toMutableMap(),
            config.getString("kafka_joark_hendelse_topic")
        )
    }
    single {
        UtsattOppgaveKafkaClient(
            utsattOppgaveLocalProperties().toMutableMap(),
            config.getString("kafka_utsatt_oppgave_topic")
        )
    }
    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get()) }
    single { IMStatsRepoImpl(get()) } bind IMStatsRepo::class
    single { DatapakkePublisherJob(get(), get(), config.getString("datapakke.api_url"), config.getString("datapakke.id"), om = get()) }
    single { BehandlendeEnhetConsumer(get(), get(), get()) } bind BehandlendeEnhetConsumer::class
    single { DuplikatRepositoryImpl(get()) } bind DuplikatRepository::class
    single { UtsattOppgaveDAO(UtsattOppgaveRepositoryMockk()) }
    single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get(), get()) } bind OppgaveClient::class
    single { UtsattOppgaveService(get(), get(), get(), get(), get(), get()) } bind UtsattOppgaveService::class
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
    single { JournalpostService(get(), get(), get(), get(), get(), get()) } bind JournalpostService::class
    single { InntektsmeldingService(InntektsmeldingRepositoryImp(get()), get()) } bind InntektsmeldingService::class
    single { SakClient(config.getString("opprett_sak_url"), get(), get()) } bind SakClient::class
    single { SaksbehandlingService(get(), get(), get()) } bind SaksbehandlingService::class
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

    single { ArbeidsgiverperiodeRepositoryImp(get()) } bind ArbeidsgiverperiodeRepository::class

    single { MockBrregClient() } bind BrregClient::class
}
