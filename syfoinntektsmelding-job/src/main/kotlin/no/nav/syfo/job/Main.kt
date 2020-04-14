package no.nav.syfo.job

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.Properties

val log = LoggerFactory.getLogger("no.nav.syfo.job")

fun main() {
    val env = System.getenv()
    rapportJob(env)
}

val objectMapper = ObjectMapper()

private fun rapportJob(env: Map<String, String>) {
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> LoggerFactory.getLogger("no.nav.syfo").error(throwable.message, throwable) }
    val serviceUser = readServiceUserCredentials()
    val environment = setUpEnvironment()

    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource(DataSourceBuilder.Role.ReadOnly)
    val dao = UtsattOppgaveDao(dataSource)
    val utsattOppgaveProducer =
        KafkaProducer<String, UtsattOppgaveDTO>(loadBaseConfig(environment, serviceUser).toProducerConfig())

    dao.finnUtgåtteOppgaver()
        .forEach { utsattOppgave ->
            if (utsattOppgave.skalTimeUt()) {
                log.info("fant oppgave som har timet ut for inntektsmelding: ${utsattOppgave.inntektsmeldingId}" +
                    ", i tilstand: ${utsattOppgave.tilstand}, med timeout: ${utsattOppgave.timeout}")
                utsattOppgaveProducer.send(ProducerRecord("topic", "key", utsattOppgave.tilDTO()))
            }
        }
}

fun loadBaseConfig(env: Environment, serviceUser: ServiceUser): Properties = Properties().also {
    it[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
    it[SaslConfigs.SASL_MECHANISM] = "PLAIN"
    it[SaslConfigs.SASL_JAAS_CONFIG] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
        "username=\"${serviceUser.username}\" password=\"${serviceUser.password}\";"
    it[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = env.kafkaBootstrapServers
}

fun Properties.toProducerConfig(): Properties = Properties().also {
    it.putAll(this)
    it[ProducerConfig.ACKS_CONFIG] = "all"
    it[ProducerConfig.CLIENT_ID_CONFIG] = "spre-arbeidsgiver-v1"
    it[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    it[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = UtsattOppgaveSerializer::class.java
}


