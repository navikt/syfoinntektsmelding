package no.nav.syfo.kafkamottak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDateTime


private val om: ObjectMapper = jacksonObjectMapper()
    .registerModule(KotlinModule())
    .registerModule(JavaTimeModule())


class JoarkHendelseConsumer() {
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository = TODO()

    @KtorExperimentalAPI
    fun listen(cr: ConsumerRecord<String, GenericRecord>, acknowledgment: Acknowledgment) {
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
        bakgrunnsjobbRepo.save(
            Bakgrunnsjobb(
                data = om.writeValueAsString(hendelse),
                type = JoarkInntektsmeldingHendelseProsessor.JOBB_TYPE,
                kjoeretid = LocalDateTime.now(),
                maksAntallForsoek = 10
            )
        )

        acknowledgment.acknowledge()
    }
}
