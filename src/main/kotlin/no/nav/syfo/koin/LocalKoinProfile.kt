
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
import no.nav.syfo.config.*

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

    //single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class

    single { KafkaConsumerConfigs(config.getString("kafka_bootstrap_servers"), config.getString("srvsyfoinntektsmelding.username"), config.getString("srvsyfoinntektsmelding.password"))} bind KafkaConsumerConfigs::class
    single { OppgaveClientConfigProvider(config.getString("oppgavebehandling.url"), config.getString("securitytokenservice.url"), config.getString("srvappserver.username"), config.getString("srvappserver.password")) }
    single { SakClientConfigProvider(config.getString("opprett_sak_url"), config.getString("securitytokenservice.url"), config.getString("srvappserver.username"), config.getString("srvappserver.password")) }
    single { VaultHikariConfig(config.getString("vault.enabled:true").toBoolean(), config.getString("vault.backend"), config.getString("vault.role:syfoinntektsmelding-user"), config.getString("vault.admin:syfoinntektsmelding-admin") ) }
}
