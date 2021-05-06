package no.nav.syfo.slowtests.kafka

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.TopicExistsException
import java.util.*
import java.util.concurrent.TimeUnit

class KafkaAdminForTests(private val props : MutableMap<String, Any>, private val topicName: String) {

    private lateinit var adminClient: AdminClient

    fun createTopicIfNotExists() {
        try {
            adminClient = KafkaAdminClient.create(props)
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

    fun addRecordeToKafka(record : String, topicName : String, props : Properties) {
        val kafkaproducer = KafkaProducer<String, String>(props)
        val res = kafkaproducer.send(ProducerRecord(topicName, record)).get()
        kafkaproducer.flush()
        kafkaproducer.close()
    }
}

