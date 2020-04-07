package no.nav.syfo.service

import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.repository.UtsattOppgaveService
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDateTime
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class OppgaveServiceTest {

    @Mock
    private lateinit var oppgaveClient: OppgaveClient

    @Mock
    private lateinit var behandlendeEnhetConsumer: BehandlendeEnhetConsumer

    @Mock
    private lateinit var oppgaveDao: UtsattOppgaveService

    @InjectMocks
    private lateinit var oppgaveService: OppgaveService

    private val fnr = "fnr"
    private val saksId = "saksId"
    private val aktørId = "aktørId"
    private val journalpostId = "journalpostId"
    private val arkivreferanse = "123"

    @Test
    fun `oppretter forsinket oppgave med timeout`() {
        val timeout = LocalDateTime.of(2020, 4, 6, 9, 0)
        val oppgave = FremtidigOppgave(
            fnr = fnr,
            saksId = saksId,
            aktørId = aktørId,
            journalpostId = journalpostId,
            arkivreferanse = arkivreferanse,
            timeout = timeout,
            inntektsmeldingId = UUID.randomUUID()
        )
        oppgaveService.planleggOppgave(oppgave)
        verify(oppgaveDao).opprett(oppgave)
    }
}
