package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import org.apache.kafka.clients.consumer.ConsumerRecords
//import no.nav.helse.inntektsmeldingsvarsel.ANTALL_INNKOMMENDE_MELDINGER
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

interface MeldingProvider {
    fun getMessagesToProcess(): List<String>
    fun confirmProcessingDone()
}

class JoarkHendelseKafkaClient(props: MutableMap<String, Any>,
                               topicName: String,
                               private val om : ObjectMapper,
                               private val bakgrunnsjobbRepo: BakgrunnsjobbRepository) : MeldingProvider,
        LivenessComponent {

    private var currentBatch: List<String> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String>
    private val  topicPartition = TopicPartition(topicName, 0)

    private val log = LoggerFactory.getLogger(JoarkHendelseKafkaClient::class.java)

    init {
        consumer = KafkaConsumer<String, String>(props, StringDeserializer(), StringDeserializer())
        consumer.assign(Collections.singletonList(topicPartition))

        Runtime.getRuntime().addShutdownHook(Thread {
            log.debug("Got shutdown message, closing Kafka connection...")
            consumer.close()
            log.debug("Kafka connection closed")
        })
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<String> {
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }

        try {
            val records : ConsumerRecords<String, String>? = consumer.poll(Duration.ofMillis(100))
            val payloads = records?.map { it.value() }
            payloads.let {  currentBatch = it!! }
            records?.forEach { record ->
                val hendelse = om.readValue(record.value().toString(),InngaaendeJournalpostDTO::class.java)

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

            lastThrown = null

            log.debug("Fikk ${records?.count()} meldinger med offsets ${records?.map { it.offset() }?.joinToString(", ")}")
            return payloads!!
        } catch (e: Exception) {
            lastThrown = e
            throw e
        }
    }

    override fun confirmProcessingDone() {
        consumer.commitSync()
//        ANTALL_INNKOMMENDE_MELDINGER.inc(currentBatch.size.toDouble())
        currentBatch = emptyList()
    }

    override suspend fun runLivenessCheck() {
        lastThrown?.let { throw lastThrown as Exception }
    }
}

