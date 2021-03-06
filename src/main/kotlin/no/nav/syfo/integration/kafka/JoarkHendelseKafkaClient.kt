package no.nav.syfo.integration.kafka

//import no.nav.helse.inntektsmeldingsvarsel.ANTALL_INNKOMMENDE_MELDINGER
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

interface MeldingProvider {
    fun getMessagesToProcess(): List<String>
    fun confirmProcessingDone()
}

class JoarkHendelseKafkaClient(props: MutableMap<String, Any>, topicName: String) : MeldingProvider, LivenessComponent {
    private var currentBatch: List<String> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, GenericRecord> = KafkaConsumer(props)
    private val  topicPartition = TopicPartition(topicName, 0)

    private val log = LoggerFactory.getLogger(JoarkHendelseKafkaClient::class.java)

    init {
        consumer.assign(Collections.singletonList(topicPartition))

        Runtime.getRuntime().addShutdownHook(Thread {
            log.debug("Got shutdown message, closing Kafka connection...")
            stop()
            log.debug("Kafka connection closed")
        })
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<String> {
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }
        try {
            val records = consumer.poll(Duration.ofMillis(100))
            currentBatch = records.map { it.value().toString() }

            lastThrown = null

            log.debug("Fikk ${records?.count()} meldinger med offsets ${records?.map { it.offset() }?.joinToString(", ")}")
            return currentBatch
        } catch (e: Exception) {
            stop()
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

