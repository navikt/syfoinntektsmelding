package no.nav.syfo.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.hag.utils.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.saf.SafDokumentClient
import no.nav.syfo.client.saf.SafJournalpostClient
import no.nav.syfo.integration.kafka.UtsattOppgaveConsumer
import no.nav.syfo.integration.kafka.inntektsmeldingFraSimbaLocalProperties
import no.nav.syfo.integration.kafka.joarkLocalProperties
import no.nav.syfo.integration.kafka.journalpost.JournalpostHendelseConsumer
import no.nav.syfo.integration.kafka.producerLocalProperties
import no.nav.syfo.integration.kafka.utsattOppgaveLocalProperties
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.ArbeidsgiverperiodeRepository
import no.nav.syfo.repository.ArbeidsgiverperiodeRepositoryImp
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingRepositoryImp
import no.nav.syfo.repository.InntektsmeldingRepositoryMock
import no.nav.syfo.repository.UtsattOppgaveRepository
import no.nav.syfo.repository.UtsattOppgaveRepositoryImp
import no.nav.syfo.repository.UtsattOppgaveRepositoryMockk
import no.nav.syfo.repository.createHikariConfig
import no.nav.syfo.service.BehandleInngaaendeJournalConsumer
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.service.InngaaendeJournalConsumer
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.service.JournalConsumer
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.simba.InntektsmeldingConsumer
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.getString
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

fun localDevConfig(config: ApplicationConfig) =
    module {
        mockExternalDependecies()

        single { InntektsmeldingRepositoryMock() } bind InntektsmeldingRepository::class

        single {
            HikariDataSource(
                createHikariConfig(
                    config.getjdbcUrlFromProperties(),
                    config.getString("database.username"),
                    config.getString("database.password"),
                ),
            )
        } bind DataSource::class
        single { UtsattOppgaveRepositoryImp(get()) } bind UtsattOppgaveRepository::class

        single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get(), get(), get(), get()) }

        single {
            JournalpostHendelseConsumer(
                joarkLocalProperties(),
                config.getString("kafka_joark_hendelse_topic"),
                get(),
                get(),
            )
        }
        single {
            UtsattOppgaveConsumer(utsattOppgaveLocalProperties(), config.getString("kafka_utsatt_oppgave_topic"), get(), get(), get())
        }
        single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
        single { BakgrunnsjobbService(get()) }
        single { BehandlendeEnhetConsumer(get(), get(), get()) }
        single { UtsattOppgaveDAO(UtsattOppgaveRepositoryMockk()) }
        single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get()) { "local token" } }
        single { UtsattOppgaveService(get(), get(), get(), get(), get(), get()) }
        single { FeiletUtsattOppgaveMeldingProsessor(get(), get()) }

        single { FjernInntektsmeldingByBehandletProcessor(get(), 1) }

        single { InntektsmeldingBehandler(get(), get(), get(), get(), get(), get()) }
        single { InngaaendeJournalConsumer(get()) }
        single { BehandleInngaaendeJournalConsumer(get()) }
        single { JournalConsumer(get(), get(), get()) }
        single { Metrikk() } bind Metrikk::class
        single { JournalpostService(get(), get(), get(), get(), get()) }
        single { InntektsmeldingService(InntektsmeldingRepositoryImp(get()), get()) }
        single { JoarkInntektsmeldingHendelseProsessor(get(), get(), get(), get()) }

        single {
            InntektsmeldingAivenProducer(producerLocalProperties(config.getString("kafka_bootstrap_servers")))
        }

        single {
            SafJournalpostClient(
                get(),
                "http://localhost",
                ::fakeToken,
            )
        }

        single {
            SafDokumentClient(
                config.getString("saf_dokument_url"),
                get(),
                ::fakeToken,
            )
        }

        single {
            DokArkivClient(
                config.getString("dokarkiv_url"),
                get(),
                ::fakeToken,
            )
        }

        single { ArbeidsgiverperiodeRepositoryImp(get()) } bind ArbeidsgiverperiodeRepository::class

        single {
            InntektsmeldingConsumer(
                inntektsmeldingFraSimbaLocalProperties(),
                "inntektsmelding",
                get(),
                get(),
                get(),
                get(),
            )
        }
    }

private fun fakeToken(): String = "fake token"
