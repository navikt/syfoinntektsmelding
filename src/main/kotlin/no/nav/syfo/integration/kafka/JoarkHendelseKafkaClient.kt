package no.nav.syfo.integration.kafka

import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.util.logger
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

interface JoarkHendelseProvider {
    fun getMessagesToProcess(): List<InngaaendeJournalpostDTO>
    fun confirmProcessingDone()
}

class JoarkHendelseKafkaClient(props: Map<String, Any>, topicName: String) : JoarkHendelseProvider, LivenessComponent {
    private var currentBatch: List<InngaaendeJournalpostDTO> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, GenericRecord> = KafkaConsumer(props)
    private val logger = this.logger()
    private var isOpen = false
    private var topic = topicName

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.debug("Got shutdown message, closing Kafka connection...")
                try {
                    stop()
                } catch (e: ConcurrentModificationException) {
                    logger.debug("Fikk ConcurrentModificationException når man stoppet")
                }
                logger.debug("Kafka connection closed")
            }
        )
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<InngaaendeJournalpostDTO> {
        if (!isOpen) {
            logger.info("Subscribing to topic $topic ...")
            consumer.subscribe(listOf(topic))
            logger.info("Successfully subscribed to topic $topic")
            isOpen = true
        }
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }
        try {

            val records = consumer.poll(Duration.ofMillis(1000))
            currentBatch = records.map { mapInngaaendeJournalpostDTO(it.value()) }
            lastThrown = null
            if (records?.count() != null && records.count() > 0) {
                logger.info("JoarkHendelse: Fikk ${records.count()} meldinger med offsets ${records.map { it.offset() }.joinToString(", ")}")
            }
            return currentBatch
        } catch (e: Exception) {
            lastThrown = e
            throw e
        }
    }

    override fun confirmProcessingDone() {
        consumer.commitSync()
        currentBatch = emptyList()
    }

    override suspend fun runLivenessCheck() {
        lastThrown?.let { throw lastThrown as Exception }
    }
}
