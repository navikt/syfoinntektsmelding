package no.nav.syfo.integration.kafka

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer

private const val LOCALHOSTBOOTSTRAPSERVER = "localhost:9092"

private fun envOrThrow(envVar: String) = System.getenv()[envVar] ?: throw IllegalStateException("$envVar er påkrevd miljøvariabel")

fun consumerLocalProperties() =
    mutableMapOf<String, Any>(
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "30000",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to LOCALHOSTBOOTSTRAPSERVER,
    )

fun joarkLocalProperties() =
    consumerLocalProperties() +
        mapOf(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to GenericAvroDeserializer::class.java,
            "schema.registry.url" to "http://kafka-schema-registry.tpa.svc.nais.local:8081",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v2",
        )

fun inntektsmeldingFraSimbaLocalProperties() =
    consumerLocalProperties() +
        mapOf(
            "schema.registry.url" to "http://kafka-schema-registry.tpa.svc.nais.local:8081",
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.CLIENT_ID_CONFIG to "syfoinntektsmelding",
            ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v1",
        )

fun joarkAivenProperties() =
    commonAivenProperties() +
        mapOf(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to GenericAvroDeserializer::class.java,
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to System.getenv("KAFKA_SCHEMA_REGISTRY"),
            SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
            SchemaRegistryClientConfig.USER_INFO_CONFIG to System.getenv(
                "KAFKA_SCHEMA_REGISTRY_USER",
            ) + ":" + System.getenv("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.CLIENT_ID_CONFIG to "syfoinntektsmelding",
            ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v1",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        )

fun utsattOppgaveLocalProperties() =
    consumerLocalProperties() +
        mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v1",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        )

fun utsattOppgaveAivenProperties() =
    commonAivenProperties() +
        mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v1",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        )

fun producerLocalProperties(bootstrapServers: String) =
    mutableMapOf<String, Any>().apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000")
        put(ProducerConfig.RETRIES_CONFIG, "2")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    }

fun commonAivenProperties() =
    mutableMapOf<String, Any>().apply {
        val pkcs12 = "PKCS12"
        val javaKeystore = "jks"

        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000")
        put(ProducerConfig.RETRIES_CONFIG, "2")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")

        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, envOrThrow("KAFKA_BROKERS"))
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
        put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
        put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, javaKeystore)
        put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, pkcs12)
        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, envOrThrow("KAFKA_TRUSTSTORE_PATH"))
        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, envOrThrow("KAFKA_CREDSTORE_PASSWORD"))
        put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, envOrThrow("KAFKA_KEYSTORE_PATH"))
        put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, envOrThrow("KAFKA_CREDSTORE_PASSWORD"))
        put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, envOrThrow("KAFKA_CREDSTORE_PASSWORD"))
    }
