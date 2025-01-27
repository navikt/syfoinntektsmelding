package no.nav.syfo.koin

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.hag.utils.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.syfo.MetrikkVarsler
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.client.oppgave.OppgaveService
import no.nav.syfo.integration.kafka.UtsattOppgaveConsumer
import no.nav.syfo.integration.kafka.commonAivenProperties
import no.nav.syfo.integration.kafka.joarkAivenProperties
import no.nav.syfo.integration.kafka.journalpost.JournalpostHendelseConsumer
import no.nav.syfo.integration.kafka.utsattOppgaveAivenProperties
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInntektsmeldingByBehandletProcessor
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.ArbeidsgiverperiodeRepository
import no.nav.syfo.repository.ArbeidsgiverperiodeRepositoryImp
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
import no.nav.syfo.util.getString
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

fun devConfig(config: ApplicationConfig) =
    module {
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
                },
            )
        } bind DataSource::class

        single {
            JoarkInntektsmeldingHendelseProsessor(
                om = get(),
                metrikk = get(),
                inntektsmeldingBehandler = get(),
                oppgaveService = get(),
            )
        }

        single {
            InntektsmeldingBehandler(
                journalpostService = get(),
                metrikk = get(),
                inntektsmeldingService = get(),
                inntektsmeldingAivenProducer = get(),
                utsattOppgaveService = get(),
                pdlClient = get(),
            )
        }

        single { InngaaendeJournalConsumer(safJournalpostClient = get()) }
        single { BehandleInngaaendeJournalConsumer(dokArkivClient = get()) }
        single { JournalConsumer(safDokumentClient = get(), safJournalpostClient = get(), pdlClient = get()) }
        single { Metrikk() }
        single { BehandlendeEnhetConsumer(pdlClient = get(), norg2Client = get(), metrikk = get()) }
        single {
            JournalpostService(
                inngaaendeJournalConsumer = get(),
                behandleInngaaendeJournalConsumer = get(),
                journalConsumer = get(),
                behandlendeEnhetConsumer = get(),
                metrikk = get(),
            )
        }
        single { InntektsmeldingRepositoryImp(ds = get()) } bind InntektsmeldingRepository::class
        single { InntektsmeldingService(repository = get(), objectMapper = get()) }
        single { ArbeidsgiverperiodeRepositoryImp(ds = get()) } bind ArbeidsgiverperiodeRepository::class

        single {
            JournalpostHendelseConsumer(
                props = joarkAivenProperties(),
                topicName = config.getString("kafka_joark_hendelse_topic"),
                bakgrunnsjobbRepo = get(),
                om = get(),
            )
        }
        single {
            UtsattOppgaveConsumer(
                props = utsattOppgaveAivenProperties(),
                topicName = config.getString("kafka_utsatt_oppgave_topic"),
                om = get(),
                utsattOppgaveService = get(),
                bakgrunnsjobbRepo = get(),
            )
        }

        single {
            InntektsmeldingAivenProducer(
                commonAivenProperties(),
            )
        }

        single { UtsattOppgaveDAO(UtsattOppgaveRepositoryImp(get())) }
        single {
            UtsattOppgaveService(
                utsattOppgaveDAO = get(),
                oppgaveService = get(),
                behandlendeEnhetConsumer = get(),
                inntektsmeldingRepository = get(),
                om = get(),
                metrikk = get(),
            )
        }
        single { FeiletUtsattOppgaveMeldingProsessor(om = get(), oppgaveService = get()) }

        single {
            FjernInntektsmeldingByBehandletProcessor(
                InntektsmeldingRepositoryImp(get()),
                config.getString("lagringstidMåneder").toInt(),
            )
        }
        single {
            FinnAlleUtgaandeOppgaverProcessor(
                utsattOppgaveDAO = get(),
                oppgaveService = get(),
                behandlendeEnhetConsumer = get(),
                metrikk = get(),
                inntektsmeldingRepository = get(),
                om = get(),
            )
        }

        single { PostgresBakgrunnsjobbRepository(dataSource = get()) } bind BakgrunnsjobbRepository::class
        single { BakgrunnsjobbService(bakgrunnsjobbRepository = get(), bakgrunnsvarsler = MetrikkVarsler()) }

        single {
            OppgaveService(
                oppgaveClient = get(),
                metrikk = get(),
            )
        }

// TODO: trekk ut topic og consumerConfig-properties
        single {
            InntektsmeldingConsumer(
                props =
                    commonAivenProperties() +
                        mapOf(
                            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
                            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
                            ConsumerConfig.CLIENT_ID_CONFIG to "syfoinntektsmelding-im-consumer",
                            ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-im-v1",
                            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                        ),
                topicName = "helsearbeidsgiver.inntektsmelding",
                inntektsmeldingService = get(),
                inntektsmeldingAivenProducer = get(),
                utsattOppgaveService = get(),
                pdlClient = get(),
            )
        }
    }
