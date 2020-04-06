package no.nav.syfo.service

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.SYKEPENGER_UTLAND
import no.nav.syfo.repository.UtsattOppgaveDAO
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val oppgaveDao: UtsattOppgaveDAO
) {

    @KtorExperimentalAPI
    fun opprettOppgave(fnr: String, aktorId: String, saksId: String, journalpostId: String) {
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(fnr)
        val gjelderUtland = (SYKEPENGER_UTLAND == behandlendeEnhet)
        runBlocking {
            oppgaveClient.opprettOppgave(
                sakId = saksId,
                journalpostId = journalpostId,
                tildeltEnhetsnr =  behandlendeEnhet,
                aktoerId = aktorId,
                gjelderUtland = gjelderUtland
            )
        }
    }

    fun planleggOppgave(arkivreferanse: String, timeout: LocalDateTime) {
        oppgaveDao.opprett(arkivreferanse, timeout)
    }

    fun slett(planlagtOppgave: PlanlagtOppgave) {
        oppgaveDao.slett(planlagtOppgave.arkivreferanse)
    }
}

data class PlanlagtOppgave(val arkivreferanse: String) {

}
