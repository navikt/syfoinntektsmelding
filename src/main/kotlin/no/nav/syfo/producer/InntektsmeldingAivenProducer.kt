package no.nav.syfo.producer

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.util.Metrikk
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class InntektsmeldingAivenProducer() {

    private const val JAVA_KEYSTORE = "jks"
    private const val PKCS12 = "PKCS12"


    private val producerProperties = Properties().apply {
        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000")
        put(ProducerConfig.RETRIES_CONFIG, "2")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")


        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to envOrThrow("KAFKA_BROKERS"),
        ProducerConfig.ACKS_CONFIG to "all",

        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
        SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", //Disable server host name verification
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to JAVA_KEYSTORE,
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to PKCS12,
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to envOrThrow("KAFKA_TRUSTSTORE_PATH"),
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD"),
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to envOrThrow("KAFKA_KEYSTORE_PATH"),
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD"),
        SslConfigs.SSL_KEY_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD")

    }

    private val inntektsmeldingTopics = listOf("helsearbeidsgiver.privat-sykepenger-inntektsmelding")
    val objectMapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()

    private val kafkaproducer = KafkaProducer<String, String>(producerProperties)

    fun leggMottattInntektsmeldingPåTopics(inntektsmelding: Inntektsmelding) {
        inntektsmeldingTopics.forEach {
            leggMottattInntektsmeldingPåTopic(inntektsmelding, it)
        }
    }

    private fun leggMottattInntektsmeldingPåTopic(inntektsmelding: Inntektsmelding, topic: String) {
        kafkaproducer.send(ProducerRecord(topic, inntektsmelding.arbeidstakerFnr, serialiseringInntektsmelding(inntektsmelding)))
    }

    fun serialiseringInntektsmelding(inntektsmelding: Inntektsmelding) =
        objectMapper.writeValueAsString(inntektsmelding)
}

private fun envOrThrow(envVar: String) = System.getenv()[envVar] ?: throw IllegalStateException("$envVar er påkrevd miljøvariabel")
