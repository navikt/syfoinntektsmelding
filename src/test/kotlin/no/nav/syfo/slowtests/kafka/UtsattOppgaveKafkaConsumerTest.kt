package no.nav.syfo.slowtests.kafka

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.syfo.integration.kafka.joarkLocalProperties
import no.nav.syfo.integration.kafka.producerLocalProperties
import no.nav.syfo.integration.kafka.utsattOppgaveLocalProperties
import no.nav.syfo.uttsattOpgaveKakaData
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class UtsattOppgaveKafkaConsumerTest {
    private lateinit var kafkaProdusent: KafkaAdminForTests
    private val topicName = "aapen-helse-spre-oppgaver"
    val objectMapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()

    @BeforeAll
    internal fun setUp() {
        kafkaProdusent = KafkaAdminForTests(utsattOppgaveLocalProperties().toMutableMap(),topicName )
        kafkaProdusent = KafkaAdminForTests(joarkLocalProperties().toMutableMap(),topicName )
        kafkaProdusent.createTopicIfNotExists()
        kafkaProdusent.addRecordeToKafka(objectMapper.writeValueAsString(uttsattOpgaveKakaData),
            topicName,
            producerLocalProperties( "localhost:9092"))
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.deleteTopicAndCloseConnection()
    }

    @Test
    fun `Skal lese utsattoppgave`() {

    }
}
