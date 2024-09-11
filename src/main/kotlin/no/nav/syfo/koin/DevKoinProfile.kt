package no.nav.syfo.koin

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.ApplicationConfig
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.hag.utils.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
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
import org.koin.core.qualifier.named
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
                get(),
                get(),
                get(),
                get(),
            )
        }

        single {
            InntektsmeldingBehandler(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }

        single { InngaaendeJournalConsumer(get()) }
        single { BehandleInngaaendeJournalConsumer(get()) }
        single { JournalConsumer(get(), get(), get()) }
        single { Metrikk() }
        single { BehandlendeEnhetConsumer(get(), get(), get()) }
        single { JournalpostService(get(), get(), get(), get(), get()) }
        single { InntektsmeldingRepositoryImp(get()) } bind InntektsmeldingRepository::class
        single { InntektsmeldingService(get(), get()) }
        single { ArbeidsgiverperiodeRepositoryImp(get()) } bind ArbeidsgiverperiodeRepository::class

        single {
            JournalpostHendelseConsumer(
                joarkAivenProperties(),
                config.getString("kafka_joark_hendelse_topic"),
                get(),
                get(),
            )
        }
        single {
            UtsattOppgaveConsumer(
                utsattOppgaveAivenProperties(),
                config.getString("kafka_utsatt_oppgave_topic"),
                get(),
                get(),
                get(),
            )
        }

        single {
            InntektsmeldingAivenProducer(
                commonAivenProperties(),
            )
        }

        single { UtsattOppgaveDAO(UtsattOppgaveRepositoryImp(get())) }
        single { UtsattOppgaveService(get(), get(), get(), get(), get(), get()) }
        single { FeiletUtsattOppgaveMeldingProsessor(get(), get()) }

        single {
            FjernInntektsmeldingByBehandletProcessor(
                InntektsmeldingRepositoryImp(get()),
                config.getString("lagringstidMåneder").toInt(),
            )
        }
        single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get(), get(), get(), get()) }

        single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
        single { BakgrunnsjobbService(get(), bakgrunnsvarsler = MetrikkVarsler()) }

        single {
            OppgaveClient(
                config.getString("oppgavebehandling_url"),
                get(),
                get(),
                get<AccessTokenProvider>(qualifier = named(AccessScope.OPPGAVE))::getToken,
            )
        }

        single {
            PdlClientImpl(
                config.getString("pdl_url"),
                get(qualifier = named(AccessScope.PDL)),
                get(),
                get(),
            )
        } bind PdlClient::class

        single {
            Norg2Client(
                config.getString("norg2_url"),
                get(),
            )
        }

        single {
            SafJournalpostClient(
                get(),
                config.getString("saf_journal_url"),
                get<AccessTokenProvider>(qualifier = named(AccessScope.SAF))::getToken,
            )
        }

        single {
            SafDokumentClient(
                config.getString("saf_dokument_url"),
                get(),
                get<AccessTokenProvider>(qualifier = named(AccessScope.SAF))::getToken,
            )
        }

        single {
            DokArkivClient(
                config.getString("dokarkiv_url"),
                get(),
                get<AccessTokenProvider>(qualifier = named(AccessScope.DOKARKIV))::getToken,
            )
        }

// TODO: trekk ut topic og consumerConfig-properties
        single {
            InntektsmeldingConsumer(
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
                "helsearbeidsgiver.inntektsmelding",
                get(),
                get(),
                get(),
                get(),
            )
        }
    }
