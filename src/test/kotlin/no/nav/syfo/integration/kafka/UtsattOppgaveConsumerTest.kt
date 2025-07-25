package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.utsattoppgave.DokumentTypeDTO
import no.nav.syfo.utsattoppgave.OppdateringstypeDTO
import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

internal class UtsattOppgaveConsumerTest {
    lateinit var consumer: UtsattOppgaveConsumer
    var om: ObjectMapper = mockk(relaxed = true)
    var props = joarkLocalProperties().toMap()
    var utsattOppgaveService: UtsattOppgaveService = mockk(relaxed = true)
    var bakgrunnsjobbRepo: BakgrunnsjobbRepository = mockk(relaxed = true)
    val topicName = "topic"
    val timeout: LocalDateTime = LocalDateTime.now()
    val utsattOppgaveDTO = UtsattOppgaveDTO(DokumentTypeDTO.Inntektsmelding, OppdateringstypeDTO.Opprett, UUID.randomUUID(), timeout)
    val raw = "raw"

    @BeforeEach
    fun before() {
        consumer = UtsattOppgaveConsumer(props, topicName, om, utsattOppgaveService, bakgrunnsjobbRepo)
    }

    @Test
    fun `Skal behandle UtsattOppgave`() {
        consumer.behandle(utsattOppgaveDTO, raw)
        verify(exactly = 1) { utsattOppgaveService.prosesser(any()) }
    }

    @Test
    fun `Skal opprette bakgrunnsjobb dersom behandling feiler`() {
        every {
            utsattOppgaveService.prosesser(any())
        } throws RuntimeException("Feil!")
        consumer.behandle(utsattOppgaveDTO, raw)
        verify(exactly = 1) { bakgrunnsjobbRepo.save(any()) }
    }

    @Test
    fun `Helsesjekk - isready - skal gi feilmelding før oppstart`() {
        assertThrows<IllegalStateException> {
            runBlocking {
                consumer.runReadynessCheck()
            }
        }
    }

    @Test
    fun `Helsesjekk - isready - skal ikke gi feilmelding etter oppstart`() {
        consumer.setIsReady(true)
        runBlocking {
            consumer.runReadynessCheck()
        }
    }

    @Test
    fun `Helsesjekk - liveness - skal gi feilmelding når feil oppstår`() {
        consumer.setIsError(true)
        assertThrows<IllegalStateException> {
            runBlocking {
                consumer.runLivenessCheck()
            }
        }
    }

    @Test
    fun `Helsesjekk - liveness - skal ikke gi feilmelding når alt virker`() {
        consumer.setIsError(false)
        runBlocking {
            consumer.runLivenessCheck()
        }
    }
}
