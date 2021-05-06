package no.nav.syfo.slowtests.kafka

import no.nav.syfo.slowtests.SystemTestBase

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.zaxxer.hikari.HikariDataSource
import io.mockk.mockk
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.PostgresBakgrunnsjobbRepository
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.integration.kafka.UtsattOppgaveKafkaClient
import no.nav.syfo.integration.kafka.joarkLocalProperties
import no.nav.syfo.integration.kafka.producerLocalProperties
import no.nav.syfo.integration.kafka.utsattOppgaveLocalProperties
import no.nav.syfo.repository.UtsattOppgaveRepositoryMockk
import no.nav.syfo.repository.createTestHikariConfig
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import no.nav.syfo.utsattOppgaveKakaData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class UtsattOppgaveKafkaConsumerTest : SystemTestBase() {
    private lateinit var kafkaProdusent: KafkaAdminForTests
    private val topicName = "aapen-helse-spre-oppgaver"
    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val bakrepo = PostgresBakgrunnsjobbRepository(HikariDataSource(createTestHikariConfig()))

    private val mockUtsattDAO = UtsattOppgaveDAO(UtsattOppgaveRepositoryMockk())
    val oppgaveClientMock = mockk<OppgaveClient>(relaxed = true)
    val behandlendeEnhetConsumerMock = mockk<BehandlendeEnhetConsumer>(relaxed = true)
    val utsattOppgaveServiceMock = UtsattOppgaveService(mockUtsattDAO,oppgaveClientMock, behandlendeEnhetConsumerMock)

    private val utsattmeldingConsumer = UtsattOppgaveKafkaClient(utsattOppgaveLocalProperties().toMutableMap(),
        topicName,
        objectMapper,
        utsattOppgaveServiceMock,
        bakrepo)

    @BeforeAll
    internal fun setUp() {
        kafkaProdusent = KafkaAdminForTests(utsattOppgaveLocalProperties().toMutableMap(),topicName )
        kafkaProdusent = KafkaAdminForTests(joarkLocalProperties().toMutableMap(),topicName)
        kafkaProdusent.createTopicIfNotExists()
        kafkaProdusent.addRecordeToKafka(objectMapper.writeValueAsString(utsattOppgaveKakaData),
            topicName,
            producerLocalProperties( "localhost:9092"))
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.deleteTopicAndCloseConnection()
    }

    @Test
    fun `Skal lese utsattoppgave`() {
        var meldinger =  utsattmeldingConsumer.getMessagesToProcess()
        Assertions.assertThat(meldinger.size).isEqualTo(1)
        Assertions.assertThat(meldinger[0]).isEqualTo(objectMapper.writeValueAsString(utsattOppgaveKakaData))
    }
}
