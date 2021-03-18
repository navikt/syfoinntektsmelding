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
import no.nav.helse.fritakagp.MetrikkVarsler
import no.nav.helse.fritakagp.db.*
import no.nav.helse.fritakagp.integration.altinn.message.Clients
import no.nav.helse.fritakagp.processing.brukernotifikasjon.BrukernotifikasjonProcessor
import no.nav.helse.fritakagp.processing.gravid.krav.*
import no.nav.helse.fritakagp.processing.gravid.soeknad.*
import no.nav.helse.fritakagp.processing.kronisk.krav.*
import no.nav.helse.fritakagp.processing.kronisk.soeknad.*
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    externalSystemClients(config)

    single {
        HikariDataSource(
            createHikariConfig(
                config.getjdbcUrlFromProperties(),
                config.getString("database.username"),
                config.getString("database.password")
            )
        )
    } bind DataSource::class

    single { PostgresGravidSoeknadRepository(get(), get()) } bind GravidSoeknadRepository::class
    single { PostgresKroniskSoeknadRepository(get(), get()) } bind KroniskSoeknadRepository::class
    single { PostgresGravidKravRepository(get(), get()) } bind GravidKravRepository::class
    single { PostgresKroniskKravRepository(get(), get()) } bind KroniskKravRepository::class


    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get(), bakgrunnsvarsler = MetrikkVarsler()) }

    single { GravidSoeknadProcessor(get(), get(), get(), get(), get(), GravidSoeknadPDFGenerator(), get(), get(), get()) }
    single { GravidKravProcessor(get(), get(), get(), get(), get(), GravidKravPDFGenerator(), get(), get(), get()) }

    single { KroniskSoeknadProcessor(get(), get(), get(), get(), get(), KroniskSoeknadPDFGenerator(), get(), get(), get()) }
    single { KroniskKravProcessor(get(), get(), get(), get(), get(), KroniskKravPDFGenerator(), get(), get(), get()) }

    single { Clients.iCorrespondenceExternalBasic(config.getString("altinn_melding.altinn_endpoint")) }
    
    single {
        GravidSoeknadAltinnKvitteringSender(
            config.getString("altinn_melding.service_id"),
            get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password")
        )
    } bind GravidSoeknadKvitteringSender::class

    single { GravidSoeknadKvitteringProcessor(get(), get(), get()) }    
    
    single {
        GravidKravAltinnKvitteringSender(
            config.getString("altinn_melding.service_id"),
            get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password")
        )
    } bind GravidKravKvitteringSender::class

    single { GravidKravKvitteringProcessor(get(), get(), get()) }
    
    single {
        KroniskSoeknadAltinnKvitteringSender(
            config.getString("altinn_melding.service_id"),
            get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password")
        )
    } bind KroniskSoeknadKvitteringSender::class
    single { KroniskSoeknadKvitteringProcessor(get(), get(), get()) }
    
    single {
        KroniskKravAltinnKvitteringSender(
            config.getString("altinn_melding.service_id"),
            get(),
            config.getString("altinn_melding.username"),
            config.getString("altinn_melding.password")
        )
    } bind KroniskKravKvitteringSender::class
    single { KroniskKravKvitteringProcessor(get(), get(), get()) }

    single { GravidSoeknadKafkaProcessor(get(), get(), get() ) }
    single { GravidKravKafkaProcessor(get(), get(), get()) }
    single { KroniskSoeknadKafkaProcessor(get(), get(), get()) }
    single { KroniskKravKafkaProcessor(get(), get(), get()) }
    single { BrukernotifikasjonProcessor(get(), get(), get(), get(), get(), get(), config.getString("service_user.username"), 4, "https://arbeidsgiver.nav.no/fritak-agp") }

    single { DefaultAltinnAuthorizer(get()) } bind AltinnAuthorizer::class
}