package no.nav.helse.fritakagp.koin

import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.SakConsumer
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import no.nav.syfo.consumer.rest.TokenConsumer
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumer
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import org.koin.dsl.bind
import org.koin.dsl.module

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    externalSystemClients(config)


    single { AktorConsumer(get(), config.getString("srvsyfoinntektsmelding.username"), config.getString("aktoerregister_api_v1_url"), get()) } bind AktorConsumer::class
    single { TokenConsumer(get(), config.getString("security-token-service-token.url")) } bind TokenConsumer::class
    single { InntektsmeldingBehandler() } bind InntektsmeldingBehandler::class


    single { InngaaendeJournalConsumer(get()) } bind InngaaendeJournalConsumer::class
    single { BehandleInngaaendeJournalConsumer(get()) } bind BehandleInngaaendeJournalConsumer::class
    single { JournalConsumer(get(), get()) } bind JournalConsumer::class
    single { Metrikk(get()) } bind Metrikk::class
    single { BehandlendeEnhetConsumer(get(), get(), get()) } bind BehandlendeEnhetConsumer::class
    single { JournalpostService(get(), get(), get(), get(), get()) } bind JournalpostService::class

    single { AzureAdTokenConsumer(get(),
        config.getString("aadaccesstoken_url"),
        config.getString("aad_syfogsak_clientid_username"),
        config.getString("aad_syfoinntektsmelding_clientid_password")) } bind AzureAdTokenConsumer::class

    single { SakConsumer(get(),
        get(),
        config.getString("aad_syfoinntektsmelding_clientid_username"),
        config.getString("sakconsumer_host_url"))} bind SakConsumer::class
    single { EksisterendeSakService(get()) } bind EksisterendeSakService::class
    single { InntektsmeldingService() }

}
