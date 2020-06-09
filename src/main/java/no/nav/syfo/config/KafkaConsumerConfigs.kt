package no.nav.syfo.config

import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer
import no.nav.syfo.utsattoppgave.InfiniteRetryKafkaErrorHandler
import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import no.nav.syfo.utsattoppgave.objectMapper
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@Configuration
@EnableKafka
class KafkaConsumerConfigs(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
    @Value("\${srvsyfoinntektsmelding.username}") private val username: String,
    @Value("\${srvsyfoinntektsmelding.password}") private val password: String
) {

    fun consumerProperties(): Map<String, Any> = mapOf(
        ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v1",
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
        SaslConfigs.SASL_MECHANISM to "PLAIN",
        SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"$username\" password=\"$password\";"
    )

    fun consumerFactory(propOverrides: Map<String, Any> ): ConsumerFactory<String, String> = DefaultKafkaConsumerFactory(consumerProperties().plus(propOverrides))

    @Bean
    fun utsattOppgaveListenerContainerFactory(infiniteRetryKafkaErrorHandler: InfiniteRetryKafkaErrorHandler): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setErrorHandler(infiniteRetryKafkaErrorHandler)
            consumerFactory = consumerFactory(mapOf(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to UtsattOppgaveDTODeserializer::class.java
            ))
            containerProperties.apply { ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE }
        }

    @Bean
    fun joarkhendelseListenerContainerFactory(infiniteRetryKafkaErrorHandler: InfiniteRetryKafkaErrorHandler): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setErrorHandler(infiniteRetryKafkaErrorHandler)
            consumerFactory = consumerFactory(mapOf(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to GenericAvroDeserializer::class.java,
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://kafka-schema-registry.tpa.svc.nais.local:8081",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
                ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v2"
            ))
            containerProperties.apply { ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE }
        }

    class UtsattOppgaveDTODeserializer : Deserializer<UtsattOppgaveDTO> {
        override fun deserialize(topic: String, data: ByteArray): UtsattOppgaveDTO {
            return objectMapper.readValue(data)
        }
    }
}
