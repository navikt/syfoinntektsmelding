package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())

@Component
class UtsattOppgaveConsumer(val oppgaveService: UtsattOppgaveService) {

    @KtorExperimentalAPI
    @KafkaListener(
        topics = ["aapen-helse-spre-oppgaver"],
        idIsGroup = false,
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, UtsattOppgaveDTO>, acknowledgment: Acknowledgment) {
        if (cr.value().dokumentType != DokumentTypeDTO.Inntektsmelding) {
            acknowledgment.acknowledge()
            return
        }

        val utsattOppgave = cr.value()
        val oppdatering = OppgaveOppdatering(
            utsattOppgave.dokumentId,
            utsattOppgave.oppdateringstype.tilHandling(),
            utsattOppgave.timeout
        )

        oppgaveService.prosesser(oppdatering)
        acknowledgment.acknowledge()
    }

}

@Configuration
@EnableKafka
class UtsattOppgaveConsumerConfig(
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
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to UtsattOppgaveDTODeserializer::class.java,
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
        SaslConfigs.SASL_MECHANISM to "PLAIN",
        SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"$username\" password=\"$password\";"
    )

    fun consumerFactory(): ConsumerFactory<String, String> = DefaultKafkaConsumerFactory(consumerProperties())

    @Bean
    fun kafkaListenerContainerFactory(kafkaErrorHandler: KafkaErrorHandler): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setErrorHandler(kafkaErrorHandler)
            consumerFactory = consumerFactory()
            containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(30)
            containerProperties.apply { ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE }
        }

    class UtsattOppgaveDTODeserializer : Deserializer<UtsattOppgaveDTO> {
        override fun deserialize(topic: String, data: ByteArray): UtsattOppgaveDTO {
            return objectMapper.readValue(data)
        }

    }

}

fun OppdateringstypeDTO.tilHandling() = when (this) {
    OppdateringstypeDTO.Utsett -> Handling.Utsett
    OppdateringstypeDTO.Opprett -> Handling.Opprett
    OppdateringstypeDTO.Ferdigbehandlet -> Handling.Forkast
}
