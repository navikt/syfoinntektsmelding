package no.nav.syfo.service

import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.repository.UtsattOppgaveDAO
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDateTime

@RunWith(MockitoJUnitRunner::class)
class OppgaveServiceTest {

    @Mock
    private lateinit var oppgaveClient: OppgaveClient
    @Mock
    private lateinit var behandlendeEnhetConsumer: BehandlendeEnhetConsumer
    @Mock
    private lateinit var oppgaveDao: UtsattOppgaveDAO

    @InjectMocks
    private lateinit var oppgaveService: OppgaveService

    @Test
    fun `oppretter forsinket oppgave med timeout`() {
        val timeout = LocalDateTime.of(2020, 4, 6, 9, 0)
        oppgaveService.planleggOppgave("123", timeout)
        verify(oppgaveDao).opprett("123", timeout)
    }

    @Test
    fun `sletter oppgave`() {
        val planlagtOppgave = PlanlagtOppgave("123")
        oppgaveService.slett(planlagtOppgave)
        verify(oppgaveDao).slett(planlagtOppgave.arkivreferanse)
    }
}
