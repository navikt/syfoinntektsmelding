package no.nav.helse.fritakagp.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.arbeidsgiver.web.auth.DefaultAltinnAuthorizer
import no.nav.helse.fritakagp.db.*
import no.nav.helse.fritakagp.integration.kafka.*
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.*
import no.nav.helse.fritakagp.processing.gravid.soeknad.*
import no.nav.helse.fritakagp.processing.kronisk.krav.*
import no.nav.helse.fritakagp.processing.kronisk.soeknad.*
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource


@KtorExperimentalAPI
fun localDevConfig(config: ApplicationConfig) = module {

    mockExternalDependecies()

    single { HikariDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), config.getString("database.username"), config.getString("database.password"))) } bind DataSource::class
    single { PostgresGravidSoeknadRepository(get(), get()) } bind GravidSoeknadRepository::class
    single { PostgresGravidKravRepository(get(), get()) } bind GravidKravRepository::class
    single { PostgresKroniskSoeknadRepository(get(), get()) } bind KroniskSoeknadRepository::class
    single { PostgresKroniskKravRepository(get(), get()) } bind KroniskKravRepository::class

    single { SoeknadmeldingKafkaProducer(localCommonKafkaProps(), config.getString("kafka_soeknad_topic_name"), get(), StringKafkaProducerFactory())} bind SoeknadmeldingSender::class
    single { KravmeldingKafkaProducer(localCommonKafkaProps(), config.getString("kafka_krav_topic_name"), get(), StringKafkaProducerFactory()) } bind KravmeldingSender::class


}
