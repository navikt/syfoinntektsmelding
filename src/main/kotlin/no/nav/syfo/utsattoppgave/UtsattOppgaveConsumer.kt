package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.syfo.dto.BakgrunnsjobbEntitet
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.MDCOperations.MDC_CALL_ID
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
import java.time.LocalDateTime
import java.util.*

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule())

@Component
class UtsattOppgaveConsumer(
    val oppgaveService: UtsattOppgaveService,
    val bakgrunnsjobbService: BakgrunnsjobbService,
    val om: ObjectMapper) {

    @KtorExperimentalAPI
    @KafkaListener(
        topics = ["aapen-helse-spre-oppgaver"],
        idIsGroup = false,
        containerFactory = "utsattOppgaveListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, UtsattOppgaveDTO>, acknowledgment: Acknowledgment) {
        MDCOperations.putToMDC(MDC_CALL_ID, UUID.randomUUID().toString())
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

        try {
            oppgaveService.prosesser(oppdatering)
        } catch(ex: Exception) {
            bakgrunnsjobbService.opprett(BakgrunnsjobbEntitet(
                data = om.writeValueAsString(utsattOppgave),
                type = FeiletUtsattOppgaveMeldingProsessor.JOBB_TYPE,
                kjoeretid = LocalDateTime.now().plusMinutes(30),
                maksAntallForsoek = 10
            ))
        }

        acknowledgment.acknowledge()
        MDCOperations.remove(MDC_CALL_ID)
    }

}

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
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to GenericAvroDeserializer::class.java
                ///AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://kafka-schema-registry.tpa:8081"

            ))
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
