package no.nav.syfo.koin

import io.ktor.config.*
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.syfo.integration.kafka.JoarchhendlseConsumerFactory
import no.nav.syfo.integration.kafka.UtsattOppgaveConsumerFactory
import no.nav.syfo.integration.kafka.UtsattOppgaveKafkaConsumer
import no.nav.syfo.integration.kafka.onPremCommonKafkaProps
import no.nav.syfo.kafkamottak.JoarkHendelseConsumer
import org.koin.core.module.Module

fun Module.externalSystemClients(config: ApplicationConfig) {
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
