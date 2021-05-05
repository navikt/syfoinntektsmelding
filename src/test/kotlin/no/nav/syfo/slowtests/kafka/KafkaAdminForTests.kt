package no.nav.syfo.slowtests.kafka

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.TopicExistsException
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class KafkaAdminForTests {
    companion object {
        const val topicName = "spinn-test"
    }

    private val adminClient: AdminClient = KafkaAdminClient.create(consumerFakeConfig())
    fun createTopicIfNotExists() {
        try {
            adminClient
                    .createTopics(mutableListOf(NewTopic(topicName, 1, 1)))
                    .all()
                    .get(30, TimeUnit.SECONDS)
        } catch(createException: java.util.concurrent.ExecutionException) {
            if (createException.cause is TopicExistsException) {
                println("topic exists")
            } else {
                throw createException
            }
        }
    }

    fun deleteTopicAndCloseConnection() {
        try {
            adminClient
                    .deleteTopics(mutableListOf(topicName))
                    .all()
                    .get(30, TimeUnit.SECONDS)
        } catch (ex: Exception) {
            println("can't delete topic")
        }
        adminClient.close()
    }
}


class SoeknadsmeldingKafkaConsumer(props: MutableMap<String, Any>, private val topicName: String) {
    private var currentBatch: List<String> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String> =
        KafkaConsumer(props, StringDeserializer(), StringDeserializer())
    private val  topicPartition = TopicPartition(topicName, 0)

    private val log = LoggerFactory.getLogger(this::class.java)

    init {
        consumer.assign(Collections.singletonList(topicPartition))

        Runtime.getRuntime().addShutdownHook(Thread {
            log.debug("Got shutdown message, closing Kafka connection...")
            consumer.close()
            log.debug("Kafka connection closed")
        })
    }

    fun stop() = consumer.close()

    fun getMessagesToProcess(): List<String> {
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }

        try {
            val kafkaMessages = consumer.poll(Duration.ofSeconds(10))
            val payloads = kafkaMessages.map {it.value() }
            lastThrown = null
            currentBatch = payloads

            log.debug("Fikk ${kafkaMessages.count()} meldinger med offsets ${kafkaMessages.map { it.offset() }.joinToString(", ")}")
            return payloads
        } catch (e: Exception) {
            lastThrown = e
            throw e
        }
    }

    fun confirmProcessingDone() {
        consumer.commitSync()
        currentBatch = emptyList()
    }
}

