package no.nav.syfo.integration.kafka

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.ktor.config.*
import no.nav.helse.arbeidsgiver.system.getString
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

private const val LOCALHOSTBOOTSTRAPSERVER = "localhost:9092"
private fun envOrThrow(envVar: String) =
    System.getenv()[envVar] ?: throw IllegalStateException("$envVar er påkrevd miljøvariabel")

private fun consumerOnPremProperties(config: ApplicationConfig) = mutableMapOf<String, Any>(
    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to config.getString("kafka_bootstrap_servers"),
    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
    CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG to 1000,
    CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG to 5000,
    SaslConfigs.SASL_MECHANISM to "PLAIN",
    SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
        "username=\"${config.getString("srvsyfoinntektsmelding.username")}\" password=\"${envOrThrow("SRVSYFOINNTEKTSMELDING_PASSWORD")}\";"
)

private fun consumerLocalProperties() = mutableMapOf<String, Any>(
    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "30000",
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to LOCALHOSTBOOTSTRAPSERVER,
)

fun joarkLocalProperties() = consumerLocalProperties() +  mapOf(
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to GenericAvroDeserializer::class.java,
    "schema.registry.url" to "http://kafka-schema-registry.tpa.svc.nais.local:8081",
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
    ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v2")

fun joarkOnPremProperties(config: ApplicationConfig) = consumerOnPremProperties(config) +  mapOf(
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to GenericAvroDeserializer::class.java,
    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to config.getString("kafka_schema_registry_url_config"),
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
    ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v2")

fun utsattOppgaveLocalProperties() = consumerLocalProperties() + mapOf(
    ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v1",
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to UtsattOppgaveDTODeserializer::class.java
)

fun utsattOppgaveOnPremProperties(config: ApplicationConfig) = consumerOnPremProperties(config) + mapOf(
    ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v1",
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to UtsattOppgaveDTODeserializer::class.java
)

fun producerLocalProperties(bootstrapServers: String) =  Properties().apply {
    put(ProducerConfig.ACKS_CONFIG, "all")
    put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
    put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000")
    put(ProducerConfig.RETRIES_CONFIG, "2")
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
}

fun producerOnPremProperties(config: ApplicationConfig) = Properties().apply {
    put(ProducerConfig.ACKS_CONFIG, "all")
    put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
    put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000")
    put(ProducerConfig.RETRIES_CONFIG, "2")
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "${config.getString("kafka_bootstrap_servers")}")
    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
    put(SaslConfigs.SASL_MECHANISM, "PLAIN")
    val jaasCfg = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${config.getString("srvsyfoinntektsmelding.username")}\" password=\"${envOrThrow("SRVSYFOINNTEKTSMELDING_PASSWORD")}\";"
    put(SaslConfigs.SASL_JAAS_CONFIG, jaasCfg)
}
