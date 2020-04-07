package no.nav.syfo.service

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.SYKEPENGER_UTLAND
import no.nav.syfo.repository.UtsattOppgaveService
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

@Component
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val oppgaveService: UtsattOppgaveService
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
        oppgaveService.opprett(oppgave)
    }
}

data class FremtidigOppgave(
    val fnr: String,
    val saksId: String,
    val aktørId: String,
    val journalpostId: String,
    val arkivreferanse: String,
    val inntektsmeldingId: UUID,
    val timeout: LocalDateTime = LocalDateTime.now().plusHours(1)
)
