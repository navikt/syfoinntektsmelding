package no.nav.syfo.integration.kafka

import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

interface JoarkHendelseProvider {
    fun getMessagesToProcess(): List<JoarkHendelse>
    fun confirmProcessingDone()
}

class JoarkHendelseKafkaClient(props: Map<String, Any>, topicName: String) : JoarkHendelseProvider, LivenessComponent {
    private var currentBatch: List<JoarkHendelse> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, JournalfoeringHendelseRecord> = KafkaConsumer(props)
    private val log = LoggerFactory.getLogger(JoarkHendelseKafkaClient::class.java)
    private var isOpen = false
    private var topic = topicName

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.debug("Got shutdown message, closing Kafka connection...")
                try {
                    stop()
                } catch (e: ConcurrentModificationException) {
                    log.debug("Fikk ConcurrentModificationException når man stoppet")
                }
                log.debug("Kafka connection closed")
            }
        )
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<JoarkHendelse> {
        if (!isOpen) {
            log.info("Subscribing to topic $topic ...")
            consumer.subscribe(listOf(topic))
            log.info("Successfully subscribed to topic $topic")
            isOpen = true
        }
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }
        try {
            val records = consumer.poll(Duration.ofMillis(10000))
            currentBatch = records.map { JoarkHendelse(it.key(), it.value()) }
            lastThrown = null
            if (records?.count() != null && records.count() > 0) {
                log.debug("JoarkHendelse: Fikk ${records.count()} meldinger med offsets ${records.map { it.offset() }.joinToString(", ")}")
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
