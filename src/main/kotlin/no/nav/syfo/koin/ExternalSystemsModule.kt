package no.nav.syfo.koin

import io.ktor.config.*
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.arbeidsgiver.system.getString
import org.koin.core.module.Module
import org.koin.dsl.bind

fun Module.externalSystemClients(config: ApplicationConfig) {
    single { PdlClientImpl(config.getString("pdl_url"), get(), get(), get()) } bind PdlClient::class
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) } bind DokarkivKlient::class
    single { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(), get()) } bind OppgaveKlient::class

}
