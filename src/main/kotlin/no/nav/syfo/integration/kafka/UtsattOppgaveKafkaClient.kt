package no.nav.syfo.integration.kafka

//import no.nav.helse.inntektsmeldingsvarsel.ANTALL_INNKOMMENDE_MELDINGER
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration

class UtsattOppgaveKafkaClient(props: Map<String, Any>,
                               topicName: String) :
        MeldingProvider,
        LivenessComponent {

    private var currentBatch: List<String> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String> =
        KafkaConsumer<String, String>(props, StringDeserializer(), StringDeserializer())

    private val log = LoggerFactory.getLogger(UtsattOppgaveKafkaClient::class.java)

    init {
        consumer.subscribe(listOf(topicName))

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

