package no.nav.syfo.kafkamottak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.syfo.dto.BakgrunnsjobbEntitet
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.MDCOperations.MDC_CALL_ID
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
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
    fun listen(cr: ConsumerRecord<String, GenericRecord>, acknowledgment: Acknowledgment) {
        MDCOperations.putToMDC(MDC_CALL_ID, UUID.randomUUID().toString())
        val hendelse = om.readValue<InngaaendeJournalpostDTO>(cr.value().toString())

        // https://confluence.adeo.no/display/BOA/Tema https://confluence.adeo.no/display/BOA/Mottakskanal
        val isSyketemaOgFraAltinnMidlertidig =
                hendelse.temaNytt == "SYK" &&
                hendelse.mottaksKanal == "ALTINN" &&
                hendelse.journalpostStatus == "M"

        if (!isSyketemaOgFraAltinnMidlertidig) {
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
