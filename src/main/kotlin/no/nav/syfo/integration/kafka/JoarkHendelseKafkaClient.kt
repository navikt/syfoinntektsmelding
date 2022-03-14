package no.nav.syfo.integration.kafka

import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

interface JoarkHendelseProvider {
    fun getMessagesToProcess(): List<JoarkHendelse>
    fun confirmProcessingDone()
}

class JoarkHendelseKafkaClient(props: MutableMap<String, Any>, topicName: String) : JoarkHendelseProvider, LivenessComponent {
    private var currentBatch: List<JoarkHendelse> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, GenericRecord> = KafkaConsumer(props)
    private val log = LoggerFactory.getLogger(JoarkHendelseKafkaClient::class.java)

    init {
        consumer.subscribe(listOf(topicName))
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
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }
        try {
            val records = consumer.poll(Duration.ofMillis(10000))
            currentBatch = records.map { JoarkHendelse(it.key(), it.value().toString()) }
            lastThrown = null
            if (records?.count() != null && records.count() > 0) {
                log.debug("JoarkHendelse: Fikk ${records.count()} meldinger med offsets ${records.map { it.offset() }.joinToString(", ")}")
            }
            return currentBatch
        } catch (e: Exception) {
            stop()
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
