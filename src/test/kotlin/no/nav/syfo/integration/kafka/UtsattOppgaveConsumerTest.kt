package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.utsattoppgave.DokumentTypeDTO
import no.nav.syfo.utsattoppgave.OppdateringstypeDTO
import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class UtsattOppgaveConsumerTest {

    lateinit var consumer: UtsattOppgaveConsumer
    var om: ObjectMapper = mockk(relaxed = true)
    var props = joarkLocalProperties().toMap()
    var utsattOppgaveService: UtsattOppgaveService = mockk(relaxed = true)
    var bakgrunnsjobbRepo: BakgrunnsjobbRepository = mockk(relaxed = true)
    val TOPIC_NAME = "topic"
    val TIMEOUT = LocalDateTime.now()
    val utsattOppgaveDTO = UtsattOppgaveDTO(DokumentTypeDTO.Inntektsmelding, OppdateringstypeDTO.Opprett, UUID.randomUUID(), TIMEOUT)
    val RAW = "raw"

    @BeforeEach
    fun before() {
        consumer = UtsattOppgaveConsumer(props, TOPIC_NAME, om, utsattOppgaveService, bakgrunnsjobbRepo)
    }

    @Test
    fun skal_prosessere() {
        every {
            utsattOppgaveService.prosesser(any())
        } returns Unit
        consumer.behandle(utsattOppgaveDTO, RAW)
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
