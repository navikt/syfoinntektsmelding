package no.nav.syfo.kafkamottak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.syfo.dto.BakgrunnsjobbEntitet
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.MDCOperations.MDC_CALL_ID
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.utsattoppgave.InfiniteRetryKafkaErrorHandler
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
import java.util.UUID

private val om: ObjectMapper = jacksonObjectMapper()
    .registerModule(KotlinModule())
    .registerModule(JavaTimeModule())

@Component
class JoarkHendelseConsumer(
    val bakgrunnsjobbService: BakgrunnsjobbService) {

    @KtorExperimentalAPI
    @KafkaListener(
        topics = ["aapen-dok-journalfoering-v1-p"],
        idIsGroup = false,
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, InngaaendeJournalpostDTO>, acknowledgment: Acknowledgment) {
        MDCOperations.putToMDC(MDC_CALL_ID, UUID.randomUUID().toString())
        val hendelse = cr.value()
        // https://confluence.adeo.no/display/BOA/Tema https://confluence.adeo.no/display/BOA/Mottakskanal
        val isSyketemaOgFraAltinn = hendelse.temaNytt != "SYK" && hendelse.mottaksKanal == "ALTINN"

        if (!isSyketemaOgFraAltinn) {
            acknowledgment.acknowledge()
            return
        }

        bakgrunnsjobbService.opprett(BakgrunnsjobbEntitet(
            data = om.writeValueAsString(hendelse),
            type = JoarkInntektsmeldingHendelseProsessor.JOBB_TYPE,
            kjoeretid = LocalDateTime.now(),
            maksAntallForsoek = 10
        ))

        acknowledgment.acknowledge()
        MDCOperations.remove(MDC_CALL_ID)
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
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to InngaaendeJournalpostDTODeserializer::class.java,
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
        SaslConfigs.SASL_MECHANISM to "PLAIN",
        SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"$username\" password=\"$password\";"
    )

    fun consumerFactory(): ConsumerFactory<String, String> = DefaultKafkaConsumerFactory(consumerProperties())

    @Bean
    fun kafkaListenerContainerFactory(infiniteRetryKafkaErrorHandler: InfiniteRetryKafkaErrorHandler): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setErrorHandler(infiniteRetryKafkaErrorHandler)
            consumerFactory = consumerFactory()
            containerProperties.apply { ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE }
        }

    class InngaaendeJournalpostDTODeserializer : Deserializer<InngaaendeJournalpostDTO> {
        override fun deserialize(topic: String, data: ByteArray): InngaaendeJournalpostDTO {
            return om.readValue(data)
        }

    }
}
