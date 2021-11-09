package no.nav.syfo.slowtests.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.syfo.integration.kafka.JoarkHendelseKafkaClient
import no.nav.syfo.integration.kafka.joarkLocalProperties
import no.nav.syfo.integration.kafka.producerLocalProperties
import no.nav.syfo.journalPostKafkaData
import no.nav.syfo.slowtests.SystemTestBase
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("WIP: For at denne testen skal være verdifull måvi sette opp et schema registry eller en mock av en")
class JoarkHendelseKafkaConsumerTest : SystemTestBase() {
    private lateinit var kafkaProdusent: KafkaAdminForTests
    private val topicName = "aapen-dok-journalfoering-v1-q1"

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val joarkHendleseConsumer = JoarkHendelseKafkaClient(joarkLocalProperties().toMutableMap(), topicName)

    @BeforeAll
    internal fun setUp() {
        kafkaProdusent = KafkaAdminForTests(joarkLocalProperties().toMutableMap(), topicName)
        kafkaProdusent.createTopicIfNotExists()
        kafkaProdusent.addRecordeToKafka(
            objectMapper.writeValueAsString(journalPostKafkaData),
            topicName,
            producerLocalProperties("localhost:9092")
        )
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.deleteTopicAndCloseConnection()
    }

    @Test
    fun `Skal lese joark hendelse`() {
        val meldinger = joarkHendleseConsumer.getMessagesToProcess()
        Assertions.assertThat(meldinger.size).isEqualTo(1)
        Assertions.assertThat(meldinger[0]).isEqualTo(objectMapper.writeValueAsString(journalPostKafkaData))
    }
}
