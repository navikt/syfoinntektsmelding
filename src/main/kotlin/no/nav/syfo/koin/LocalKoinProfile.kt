
package no.nav.syfo.koin

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.*
import io.ktor.util.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
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
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.binding.OppgavebehandlingV3
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.InngaaendeJournalV1
import no.nav.tjeneste.virksomhet.behandlesak.v2.BehandleSakV2
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.BehandleInngaaendeJournalV1

import no.nav.syfo.consumer.util.ws.WsClientMock
import no.nav.syfo.consumer.util.ws.LogErrorHandler
import no.nav.syfo.prosesser.FjernInnteksmeldingByBehandletProcessor

@KtorExperimentalAPI
fun localDevConfig(config: ApplicationConfig) = module {
    mockExternalDependecies()

    single { AktorConsumer(get(), config.getString("srvsyfoinntektsmelding.username"), config.getString("aktoerregister_api_v1_url"), get())}
    single { TokenConsumer(get(), config.getString("security-token-service-token.url")) }
    single { InntektsmeldingRepositoryMock() } bind  InntektsmeldingRepository::class

    single { HikariDataSource(createHikariConfig(config.getjdbcUrlFromProperties(), config.getString("database.username"), config.getString("database.password"))) } bind DataSource::class
    single { FeiletRepositoryImp(get()) } bind FeiletRepository::class
    single { UtsattOppgaveRepositoryImp(get()) } bind UtsattOppgaveRepository::class

    single { FinnAlleUtgaandeOppgaverProcessor(get(), get(), get()) } bind FinnAlleUtgaandeOppgaverProcessor::class

    single { KafkaConsumerConfigs(config.getString("kafka_bootstrap_servers"), config.getString("srvsyfoinntektsmelding.username"), config.getString("srvsyfoinntektsmelding.password"))} bind KafkaConsumerConfigs::class
    single { OppgaveClientConfigProvider(config.getString("oppgavebehandling_url"), config.getString("security_token_service_token_url"), config.getString("service_user.username"), config.getString("service_user.password")) }
    single { SakClientConfigProvider(config.getString("opprett_sak_url"), config.getString("security_token_service_token_url"), config.getString("service_user.username"), config.getString("service_user.password")) }
    //single { VaultHikariConfig(config.getString("vault.enabled:true").toBoolean(), config.getString("vault.backend"), config.getString("vault.role:syfoinntektsmelding-user"), config.getString("vault.admin:syfoinntektsmelding-admin") ) }
    single { PostgresBakgrunnsjobbRepository(get()) } bind BakgrunnsjobbRepository::class
    single { BakgrunnsjobbService(get()) }

    single { Metrikk() } bind Metrikk::class

    single { WsClientMock<PersonV3>().createPort(config.getString("virksomhet_person_3_endpointurl"), PersonV3::class.java, listOf(LogErrorHandler()))} bind PersonV3::class
    single { WsClientMock<ArbeidsfordelingV1>().createPort(config.getString("virksomhet_arbeidsfordeling_v1_endpointurl"),ArbeidsfordelingV1::class.java,listOf(LogErrorHandler())) } bind ArbeidsfordelingV1::class
    single { WsClientMock<OppgavebehandlingV3>().createPort(config.getString("servicegateway_url"),OppgavebehandlingV3::class.java,listOf(LogErrorHandler()))} bind OppgavebehandlingV3::class
    single { WsClientMock<JournalV2>().createPort(config.getString("journal_v2_endpointurl"), JournalV2::class.java, listOf(LogErrorHandler())) } bind JournalV2::class
    single { WsClientMock<InngaaendeJournalV1>().createPort(config.getString("inngaaendejournal_v1_endpointurl"),InngaaendeJournalV1::class.java,listOf(LogErrorHandler())) } bind InngaaendeJournalV1::class
    single { WsClientMock<BehandleSakV2>().createPort(config.getString("virksomhet_behandlesak_v2_endpointurl"), BehandleSakV2::class.java, listOf(LogErrorHandler())) } bind BehandleSakV2::class
    single { WsClientMock<BehandleInngaaendeJournalV1>().createPort(config.getString("behandleinngaaendejournal_v1_endpointurl"),BehandleInngaaendeJournalV1::class.java,listOf(LogErrorHandler())) } bind BehandleInngaaendeJournalV1::class

    single { BehandlendeEnhetConsumer(get(),get(), get()) } bind BehandlendeEnhetConsumer::class
    single { UtsattOppgaveDAO(UtsattOppgaveRepositoryImp(get()))}
    single { OppgaveClient(config.getString("oppgavebehandling_url"), get(), get())} bind OppgaveClient::class
    single { UtsattOppgaveService(get(), get(), get()) } bind UtsattOppgaveService::class
    single { FeiletUtsattOppgaveMeldingProsessor(get(), get() )}

    single { FjernInnteksmeldingByBehandletProcessor(get(), 1)} bind FjernInnteksmeldingByBehandletProcessor::class

}
