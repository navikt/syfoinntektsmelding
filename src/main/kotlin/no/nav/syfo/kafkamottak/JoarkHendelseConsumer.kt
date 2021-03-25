package no.nav.syfo.kafkamottak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.integration.kafka.ConsumerFactory
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class JoarkHendelseConsumer(
    props: Map<String, Any>,
    private val topicName: String,
    private val om: ObjectMapper,
    consumerFactory: ConsumerFactory<Nokkel, Beskjed>,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
) {
    private val consumer = consumerFactory.createConsumer(props)

    fun consumeMessage() {
        consumer.subscribe(listOf(topicName))
        while (true) {
            val records: ConsumerRecords<Nokkel, Beskjed>? = consumer.poll(Duration.ofMillis(100))
            records?.forEach { record ->
                val hendelse = om.readValue<InngaaendeJournalpostDTO>(record.value().toString())
                // https://confluence.adeo.no/display/BOA/Tema https://confluence.adeo.no/display/BOA/Mottakskanal
                val isSyketemaOgFraAltinnMidlertidig =
                    hendelse.temaNytt == "SYK" &&
                        hendelse.mottaksKanal == "ALTINN" &&
                        hendelse.journalpostStatus == "M"

                if (!isSyketemaOgFraAltinnMidlertidig) {
                    return@forEach
                }

                bakgrunnsjobbRepo.save(
                    Bakgrunnsjobb(
                        type = JoarkInntektsmeldingHendelseProsessor.JOB_TYPE,
                        kjoeretid = LocalDateTime.now(),
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(
                            JoarkInntektsmeldingHendelseProsessor.JobbData(
                                UUID.randomUUID(),
                                om.writeValueAsString(hendelse)
                            )
                        )
                    )
                )
            }
            consumer.commitSync()
        }
    }
}
