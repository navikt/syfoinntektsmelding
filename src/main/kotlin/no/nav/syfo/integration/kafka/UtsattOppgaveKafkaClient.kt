package no.nav.syfo.integration.kafka

import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helsearbeidsgiver.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration

interface MeldingProvider {
    fun getMessagesToProcess(): List<String>
    fun confirmProcessingDone()
}

class UtsattOppgaveKafkaClient(
    props: Map<String, Any>,
    topicName: String
) :
    MeldingProvider,
    LivenessComponent {

    private var currentBatch: List<String> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String> = KafkaConsumer(props, StringDeserializer(), StringDeserializer())

    private val logger = this.logger()

    init {
        logger.info("Subscribing to topic $topicName ...")
        consumer.subscribe(listOf(topicName))
        logger.info("Successfully subscribed to topic $topicName")
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

    override fun getMessagesToProcess(): List<String> {
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }

        try {
            val records: ConsumerRecords<String, String>? = consumer.poll(Duration.ofSeconds(10))
            val payloads = records?.map { it.value() }
            payloads.let { currentBatch = it!! }

            lastThrown = null

            if (records?.count() != null && records.count() > 0) {
                logger.debug("UtsattOppgave: Fikk ${records.count()} meldinger med offsets ${records.map { it.offset() }.joinToString(", ")}")
            }
            return payloads!!
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
