package no.nav.syfo.integration.kafka

import io.ktor.config.*
import no.nav.helse.arbeidsgiver.system.getString
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer

private const val JAVA_KEYSTORE = "jks"
private const val PKCS12 = "PKCS12"
private const val LOCALHOST = "localhost:9092"
private const val GROUP_ID_CONFIG = "helsearbeidsgiver-fritakagp"

private fun envOrThrow(envVar: String) =
    System.getenv()[envVar] ?: throw IllegalStateException("$envVar er påkrevd miljøvariabel")

fun onPremCommonKafkaProps(config: ApplicationConfig) =
    mapOf(
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to config.getString("kafka_bootstrap_servers"),
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
        SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${
            config.getString(
                "service_user.username"
            )
        }\" password=\"${config.getString("service_user.password")}\";",
        SaslConfigs.SASL_MECHANISM to "PLAIN",
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName
    )

fun gcpCommonKafkaProps() = mutableMapOf<String, Any>(
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
)

fun localCommonKafkaProps() = mutableMapOf<String, Any>(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to LOCALHOST,
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.ACKS_CONFIG to "1"
)

fun consumerFakeConfig() = mutableMapOf<String, Any>(
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to LOCALHOST,
    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "30000",
    ConsumerConfig.GROUP_ID_CONFIG to GROUP_ID_CONFIG,
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest"
)
