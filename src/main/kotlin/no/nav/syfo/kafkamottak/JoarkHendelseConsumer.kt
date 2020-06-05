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
        containerFactory = "joarkhendelseListenerContainerFactory"
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
