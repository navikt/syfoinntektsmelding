package no.nav.syfo.koin

import com.zaxxer.hikari.HikariConfig
import io.ktor.config.ApplicationConfig
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.integrasjoner.RestSTSAccessTokenProvider
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.MetrikkVarsler
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.client.BrregClient
import no.nav.syfo.client.BrregClientImp
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.client.TokenConsumer
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
import no.nav.syfo.repository.DuplikatRepository
import no.nav.syfo.repository.DuplikatRepositoryImpl
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
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

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
        JoarkInntektsmeldingHendelseProsessor(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind JoarkInntektsmeldingHendelseProsessor::class

    single {
        TokenConsumer(
            get(),
            config.getString("security-token-service-token.url"),
            config.getString("srvsyfoinntektsmelding.username"),
            config.getString("srvsyfoinntektsmelding.password")
        )
    } bind TokenConsumer::class
    single { InntektsmeldingBehandler(get(), get(), get(), get(), get(), get()) } bind InntektsmeldingBehandler::class

    single { InngaaendeJournalConsumer(get()) } bind InngaaendeJournalConsumer::class
    single { BehandleInngaaendeJournalConsumer(get()) } bind BehandleInngaaendeJournalConsumer::class
    single { JournalConsumer(get(), get(), get()) } bind JournalConsumer::class
    single { Metrikk() } bind Metrikk::class
    single { BehandlendeEnhetConsumer(get(), get(), get()) } bind BehandlendeEnhetConsumer::class
    single { JournalpostService(get(), get(), get(), get(), get(), get()) } bind JournalpostService::class
    single { InntektsmeldingRepositoryImp(get()) } bind InntektsmeldingRepository::class
    single { InntektsmeldingService(get(), get()) } bind InntektsmeldingService::class
    single { ArbeidsgiverperiodeRepositoryImp(get()) } bind ArbeidsgiverperiodeRepository::class

    single {
        JournalpostHendelseConsumer(
            joarkAivenProperties().toMutableMap(),
            config.getString("kafka_joark_hendelse_topic"),
            get(),
            get(),
            get()
        )
    }
    single {
        UtsattOppgaveConsumer(
            utsattOppgaveAivenProperties(),
            config.getString("kafka_utsatt_oppgave_topic"), get(), get(), get()
        )
    }

    single { InntektsmeldingAivenProducer(commonAivenProperties()) }

    single { DuplikatRepositoryImpl(get()) } bind DuplikatRepository::class
    single { UtsattOppgaveDAO(UtsattOppgaveRepositoryImp(get())) }
    single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get(), get()) } bind OppgaveClient::class
    single { UtsattOppgaveService(get(), get(), get(), get(), get(), get()) } bind UtsattOppgaveService::class
    single { FeiletUtsattOppgaveMeldingProsessor(get(), get()) }

    single {
        FjernInntektsmeldingByBehandletProcessor(
            InntektsmeldingRepositoryImp(get()),
            config.getString("lagringstidMåneder").toInt()
        )
    } bind FjernInntektsmeldingByBehandletProcessor::class
    single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get(), get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class

    single { FeiletService(FeiletRepositoryImp(get())) } bind FeiletService::class

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
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
        Norg2Client(
            config.getString("norg2_url"),
            get(),
            RestSTSAccessTokenProvider(
                config.getString("security_token.username"),
                config.getString("security_token.password"),
                config.getString("security_token_service_token_url"),
                get()
            )::getToken
        )
    } bind Norg2Client::class

    single {
        SafJournalpostClient(
            get(),
            config.getString("saf_journal_url"),
            RestSTSAccessTokenProvider(
                config.getString("security_token.username"),
                config.getString("security_token.password"),
                config.getString("security_token_service_token_url"),
                get()
            )
        )
    } bind SafJournalpostClient::class

    single {
        SafDokumentClient(
            config.getString("saf_dokument_url"),
            get(),
            RestSTSAccessTokenProvider(
                config.getString("security_token.username"),
                config.getString("security_token.password"),
                config.getString("security_token_service_token_url"),
                get()
            )
        )
    } bind SafDokumentClient::class

    single {
        DokArkivClient(
            config.getString("dokarkiv_url"),
            RestSTSAccessTokenProvider(
                config.getString("security_token.username"),
                config.getString("security_token.password"),
                config.getString("security_token_service_token_url"),
                get()
            ),
            get()
        )
    } bind DokArkivClient::class

    single { BrregClientImp(get(qualifier = named("proxyHttpClient")), config.getString("berreg_enhet_url")) } bind BrregClient::class

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
