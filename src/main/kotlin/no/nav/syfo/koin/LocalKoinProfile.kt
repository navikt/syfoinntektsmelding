package no.nav.syfo.koin

import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import io.ktor.util.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.TokenConsumer
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.repository.*
import no.nav.syfo.util.Metrikk
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

@KtorExperimentalAPI
fun localDevConfig(config: ApplicationConfig) = module {

    mockExternalDependecies()

    single { AktorConsumer(get(), config.getString("srvsyfoinntektsmelding.username"), config.getString("aktoerregister_api_v1_url"), get())}
    single { TokenConsumer(get(), config.getString("security-token-service-token.url")) }
    single { InntektsmeldingRepositoryMock() } bind  InntektsmeldingRepository::class

    single { HikariDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), config.getString("database.username"), config.getString("database.password"))) } bind DataSource::class
    single { FeiletRepositoryImp(get()) } bind FeiletRepository::class
    single { UtsattOppgaveRepositoryImp(get()) } bind UtsattOppgaveRepository::class
    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class

    //mock? single { Metrikk } bind Metrikk::class
    //mock? single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get())} bind OppgaveClient::class
    single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class

}
