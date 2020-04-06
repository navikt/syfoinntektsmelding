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

    fun planleggOppgave(oppgave: FremtidigOppgave) {
        oppgaveDao.opprett(oppgave)
    }
}

data class FremtidigOppgave(
    val fnr: String,
    val saksId: String,
    val aktørId: String,
    val journalpostId: String,
    val arkivreferanse: String,
    val timeout: LocalDateTime = LocalDateTime.now().plusHours(1)
)

data class PlanlagtOppgave(val arkivreferanse: String) {

}
