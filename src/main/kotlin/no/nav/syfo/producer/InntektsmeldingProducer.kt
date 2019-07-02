package no.nav.syfo.producer

import no.nav.syfo.domain.Inntektsmelding
import no.nav.syfo.util.Metrikk
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Properties

@Component
class InntektsmeldingProducer(@Value("\${inntektsmelding.behandlet.topic}") private val inntektsmeldingTopic: String,
                              @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
                              private val metrikk: Metrikk) {

    private val producerProperties = Properties().apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000")
        put(ProducerConfig.RETRIES_CONFIG, "2")
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
    }

    private val kafkaproducer = KafkaProducer<String, Inntektsmelding>(producerProperties)

    fun sendBehandletInntektsmelding(inntektsmelding: Inntektsmelding) {
        kafkaproducer.send(ProducerRecord(inntektsmeldingTopic, inntektsmelding))
        //TODO
//        metrikk.tellInntektsmeldingSendt()
    }
}