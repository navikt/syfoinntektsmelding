package no.nav.syfo.koin

import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.web.auth.AltinnAuthorizer
import no.nav.helse.arbeidsgiver.web.auth.DefaultAltinnAuthorizer
import org.koin.dsl.bind
import org.koin.dsl.module

@KtorExperimentalAPI
fun preprodConfig(config: ApplicationConfig) = module {
    externalSystemClients(config)

    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class

    single { DefaultAltinnAuthorizer(get()) } bind AltinnAuthorizer::class
}

