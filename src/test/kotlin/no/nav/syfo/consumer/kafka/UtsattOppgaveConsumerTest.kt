package no.nav.syfo.consumer.kafka

import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.repository.Handling
import no.nav.syfo.repository.OppgaveOppdatering
import no.nav.syfo.repository.UtsattOppgaveService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.*

class UtsattOppgaveConsumerTest {

    private val oppgaveService: UtsattOppgaveService = mockk(relaxed = true)

    val consumer = UtsattOppgaveConsumer(oppgaveService)

    private fun utsattOppgave(
        dokumentType: DokumentTypeDTO = DokumentTypeDTO.Inntektsmelding,
        oppdateringstype: OppdateringstypeDTO = OppdateringstypeDTO.Utsett,
        id: UUID = UUID.randomUUID(),
        timeout: LocalDateTime = now()
    ) = UtsattOppgaveDTO(
        dokumentType = dokumentType,
        oppdateringstype = oppdateringstype,
        dokumentId = id,
        timeout = timeout
    )

    @Test
    fun `konsumerer meldinger med inntektsmeldinger`() {
        val ack = mockk<Acknowledgment>(relaxed = true)
        consumer.listen(
            ConsumerRecord(
                "topic",
                0,
                0,
                "key",
                utsattOppgave(dokumentType = DokumentTypeDTO.Inntektsmelding)
            ), ack
        )
        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `konsumerer meldinger med søknader`() {
        val ack = mockk<Acknowledgment>(relaxed = true)
        consumer.listen(ConsumerRecord("topic", 0, 0, "key", utsattOppgave(dokumentType = DokumentTypeDTO.Søknad)), ack)
        verify(exactly = 1) { ack.acknowledge() }
    }

    @Test
    fun `oppdatering på manglende dokument`() {
        val ack = mockk<Acknowledgment>(relaxed = true)
        val oppgave = utsattOppgave()
        val oppdatering = OppgaveOppdatering(
            id = UUID.randomUUID(),
            handling = Handling.Utsett,
            timeout = oppgave.timeout
        )
        consumer.listen(ConsumerRecord("topic", 0, 0, "key", oppgave), ack)
        verify(exactly = 1) { ack.acknowledge() }
        verify(exactly = 0) { oppgaveService.oppdater(any(), any()) }
        verify { oppgaveService.prosesser(any()) }
    }

}
