package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.integration.kafka.journalpost.JournalpostHendelseConsumer
import no.nav.syfo.repository.DuplikatRepository
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

internal class UtsattOppgaveConsumerTest {

    lateinit var consumer: UtsattOppgaveConsumer
    var om: ObjectMapper = mockk(relaxed = true)
    var props = joarkLocalProperties().toMap()
    val topicName = "topic"
    var utsattOppgaveService: UtsattOppgaveService = mockk(relaxed = true)
    var bakgrunnsjobbRepo: BakgrunnsjobbRepository = mockk(relaxed = true)

    @BeforeEach
    fun before() {
        consumer = UtsattOppgaveConsumer(props, topicName, om, utsattOppgaveService, bakgrunnsjobbRepo)
    }

    @Test
    fun skal_prosessere() {
    }

    @Test
    fun skal_opprette_bakgrunnsjobb() {
    }



    @Test
    fun isready_skal_gi_feilmelding_før_oppstart() {
        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            runBlocking {
                consumer.runReadynessCheck()
            }
        }
    }

    @Test
    fun isready_skal_ikke_gi_feilmelding_etter_oppstart() {
        consumer.setIsReady(true)
        runBlocking {
            consumer.runReadynessCheck()
        }
    }

    @Test
    fun liveness_skal_gi_feilmelding_når_feil_oppstår() {
        consumer.setIsError(true)
        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            runBlocking {
                consumer.runLivenessCheck()
            }
        }
    }

    @Test
    fun liveness_skal_ikke_gi_feilmelding_når_alt_virker() {
        consumer.setIsError(false)
        runBlocking {
            consumer.runLivenessCheck()
        }
    }

}
