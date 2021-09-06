package no.nav.syfo.slowtests.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.runBlocking
import no.nav.syfo.integration.kafka.UtsattOppgaveKafkaClient
import no.nav.syfo.integration.kafka.joarkLocalProperties
import no.nav.syfo.integration.kafka.producerLocalProperties
import no.nav.syfo.integration.kafka.utsattOppgaveLocalProperties
import no.nav.syfo.slowtests.SystemTestBase
import no.nav.syfo.utsattOppgaveKakaData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class UtsattOppgaveKafkaConsumerTest : SystemTestBase() {
    private lateinit var kafkaProdusent: KafkaAdminForTests
    private val topicName = "tbd.spre-oppgaver"
    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val utsattmeldingConsumer = UtsattOppgaveKafkaClient(utsattOppgaveLocalProperties().toMutableMap(),
        topicName)

    @BeforeAll
    internal fun setUp() {
        kafkaProdusent = KafkaAdminForTests(utsattOppgaveLocalProperties().toMutableMap(),topicName )
        kafkaProdusent = KafkaAdminForTests(joarkLocalProperties().toMutableMap(),topicName)
        kafkaProdusent.createTopicIfNotExists()
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.deleteTopicAndCloseConnection()
    }

    @Test
    fun `Skal lese utsattoppgave`() {
        val beforeCount =  utsattmeldingConsumer.getMessagesToProcess() //
        kafkaProdusent.addRecordeToKafka(objectMapper.writeValueAsString(utsattOppgaveKakaData),
            topicName,
            producerLocalProperties( "localhost:9092"))
        val meldinger =  utsattmeldingConsumer.getMessagesToProcess()
        Assertions.assertThat(meldinger.size).isEqualTo(1)
        Assertions.assertThat(meldinger[0]).isEqualTo(objectMapper.writeValueAsString(utsattOppgaveKakaData))
    }
}
