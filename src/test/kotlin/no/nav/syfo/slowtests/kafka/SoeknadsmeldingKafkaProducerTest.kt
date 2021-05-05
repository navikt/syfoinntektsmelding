package no.nav.syfo.slowtests.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.GravidTestData
import no.nav.helse.fritakagp.integration.kafka.*
import no.nav.syfo.slowtests.kafka.KafkaAdminForTests.Companion.topicName
import no.nav.helse.slowtests.systemtests.api.SystemTestBase
import no.nav.syfo.slowtests.SystemTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


internal class SoeknadsmeldingKafkaProducerTest : SystemTestBase() {

    private val om by inject<ObjectMapper>()

    private lateinit var kafkaProdusent: KafkaAdminForTests

    @BeforeAll
    internal fun setUp() {
        kafkaProdusent = KafkaAdminForTests()
    }

    @AfterAll
    internal fun tearDown() {
        kafkaProdusent.deleteTopicAndCloseConnection()
    }

    @Test
    fun getMessages() {
        val consumer = SoeknadsmeldingKafkaConsumer(consumerFakeConfig(), topicName)
        val noMessagesExpected = consumer.getMessagesToProcess()
        val producer = SoeknadmeldingKafkaProducer(localCommonKafkaProps(), topicName, om = om, StringKafkaProducerFactory())
        assertThat(noMessagesExpected).isEmpty()

        kafkaProdusent.createTopicIfNotExists()
        producer.sendMessage(GravidTestData.soeknadGravid)
        val oneMessageExpected = consumer.getMessagesToProcess()
        assertThat(oneMessageExpected).hasSize(1)

        val stillSameMEssageExpected = consumer.getMessagesToProcess()
        assertThat(stillSameMEssageExpected).hasSize(1)
        assertThat(oneMessageExpected.first()).isEqualTo(stillSameMEssageExpected.first())

        consumer.confirmProcessingDone()

        val zeroMessagesExpected = consumer.getMessagesToProcess()
        assertThat(zeroMessagesExpected).isEmpty()

        consumer.stop()
    }
}
