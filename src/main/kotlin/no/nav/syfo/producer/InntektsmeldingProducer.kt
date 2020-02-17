package no.nav.syfo.producer

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.util.Metrikk
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class InntektsmeldingProducer(@Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
                              @Value("\${srvsyfoinntektsmelding.username}") private val username: String,
                              @Value("\${srvsyfoinntektsmelding.password}") private val password: String,
                              private val metrikk: Metrikk) {

    private val producerProperties = Properties().apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000")
        put(ProducerConfig.RETRIES_CONFIG, "2")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        val jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";"
        val jaasCfg = String.format(jaasTemplate, username, password)
        put("sasl.jaas.config", jaasCfg)
    }


    private val inntektsmeldingTopics = listOf("privat-sykepenger-inntektsmelding", "helse-rapid-v1")
    val objectMapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()

    private val kafkaproducer = KafkaProducer<String, String>(producerProperties)

    fun leggMottattInntektsmeldingPåTopics(inntektsmelding: Inntektsmelding) {
        inntektsmeldingTopics.forEach {
            leggMottattInntektsmeldingPåTopic(inntektsmelding, it)
        }
        metrikk.tellInntektsmeldingLagtPåTopic()
    }

    private fun leggMottattInntektsmeldingPåTopic(inntektsmelding: Inntektsmelding, topic: String) {
        kafkaproducer.send(ProducerRecord(topic, inntektsmelding.arbeidstakerFnr, serialiseringInntektsmelding(inntektsmelding)))
    }

    fun serialiseringInntektsmelding(inntektsmelding: Inntektsmelding) =
        objectMapper.writeValueAsString(inntektsmelding)
}
