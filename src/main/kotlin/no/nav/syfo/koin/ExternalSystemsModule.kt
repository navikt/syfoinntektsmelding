package no.nav.syfo.koin

import io.ktor.config.*
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlient
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.DokarkivKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlient
import no.nav.helse.arbeidsgiver.integrasjoner.oppgave.OppgaveKlientImpl
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClientImpl
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.integration.kafka.JoarchhendlseConsumerFactory
import no.nav.syfo.integration.kafka.UtsattOppgaveConsumerFactory
import no.nav.syfo.integration.kafka.UtsattOppgaveKafkaConsumer
import no.nav.syfo.integration.kafka.onPremCommonKafkaProps
import no.nav.syfo.kafkamottak.JoarkHendelseConsumer
import org.koin.core.module.Module
import org.koin.dsl.bind

fun Module.externalSystemClients(config: ApplicationConfig) {
    single { PdlClientImpl(config.getString("pdl_url"), get(), get(), get()) } bind PdlClient::class
    single { DokarkivKlientImpl(config.getString("dokarkiv.base_url"), get(), get()) } bind DokarkivKlient::class
    single { OppgaveKlientImpl(config.getString("oppgavebehandling.url"), get(), get()) } bind OppgaveKlient::class
    single { UtsattOppgaveKafkaConsumer(
        onPremCommonKafkaProps(config),
        config.getString("kafka_utsatt_oppgave_topic"),
        get(),
        UtsattOppgaveConsumerFactory(),get(), get()) }
    single { JoarkHendelseConsumer(
        onPremCommonKafkaProps(config),
        config.getString("kafka_joark_hendelse_topics"),
        get(),
        JoarchhendlseConsumerFactory(config.getString("kafka_schema_registry_url_config")), get()) }

}
