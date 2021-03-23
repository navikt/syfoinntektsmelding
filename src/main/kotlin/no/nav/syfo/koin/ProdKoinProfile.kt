package no.nav.helse.fritakagp.koin

import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.SakConsumer
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.rest.SakClient
import no.nav.syfo.consumer.rest.TokenConsumer
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumer
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInnteksmeldingByBehandletProcessor
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingRepositoryImp
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.repository.UtsattOppgaveRepositoryImp
import no.nav.syfo.service.EksisterendeSakService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.koin.dsl.bind
import org.koin.dsl.module

@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    externalSystemClients(config)


    single { AktorConsumer(get(), config.getString("srvsyfoinntektsmelding.username"), config.getString("aktoerregister_api_v1_url"), get()) } bind AktorConsumer::class
    single { TokenConsumer(get(), config.getString("security-token-service-token.url")) } bind TokenConsumer::class
    single { InntektsmeldingBehandler(get(), get(),get(), get(), get(), get(), get()) } bind InntektsmeldingBehandler::class


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
    single { InntektsmeldingService(InntektsmeldingRepositoryImp(get()),get(), get())} bind InntektsmeldingRepository::class
    single { SakClient(config.getString("opprett_sak_url"), get()) } bind SakClient::class
    single { SaksbehandlingService(get(), get(), get(), get()) } bind SaksbehandlingService::class
    single { InntektsmeldingProducer(
        config.getString("spring_kafka_bootstrap_servers"),
        config.getString("srvsyfoinntektsmelding.username"),
        config.getString("srvsyfoinntektsmelding.password"), get()) } bind InntektsmeldingProducer::class

    single { UtsattOppgaveDAO(UtsattOppgaveRepositoryImp(get()))} bind UtsattOppgaveDAO::class
    single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get())} bind OppgaveClient::class
    single { UtsattOppgaveService(get(), get(), get(), get(), get()) } bind UtsattOppgaveService::class

    single { FjernInnteksmeldingByBehandletProcessor(InntektsmeldingRepositoryImp(get()), config.getString("lagringstidMåneder").toInt() )} bind FjernInnteksmeldingByBehandletProcessor::class
    single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class


}
