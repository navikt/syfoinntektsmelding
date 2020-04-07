package no.nav.syfo.utsattoppgave

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.SYKEPENGER_UTLAND
import no.nav.syfo.helpers.log
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@KtorExperimentalAPI
@Service
class UtsattOppgaveService(
    private val utsattOppgaveDao: UtsattOppgaveDao,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer
) {
    fun prosesser(oppdatering: OppgaveOppdatering) {
        val oppgave = utsattOppgaveDao.finn(oppdatering.id)
        if (oppgave == null) {
            log.warn("Mottok oppdatering på en ukjent oppgave")
            return
        }

        if ((oppgave.tilstand == Tilstand.Ny || oppgave.tilstand == Tilstand.Utsatt) && oppdatering.handling == Handling.Utsett) {
            oppdatering.timeout ?: error("Timeout på utsettelse mangler")
            oppdater(oppgave.copy(tilstand = Tilstand.Utsatt, timeout = oppdatering.timeout))
            return
        }

        if ((oppgave.tilstand == Tilstand.Ny || oppgave.tilstand == Tilstand.Utsatt) && oppdatering.handling == Handling.Forkast) {
            oppdater(oppgave.copy(tilstand = Tilstand.Forkastet))
            return
        }

        if ((oppgave.tilstand == Tilstand.Ny || oppgave.tilstand == Tilstand.Utsatt) && oppdatering.handling == Handling.Opprett) {
            opprettOppgaveIGosys(oppgave)
            oppdater(oppgave.copy(tilstand = Tilstand.Opprettet))
            return
        }

        log.info("Oppdatering på dokumentId: ${oppdatering.id} ikke relevant")
    }

    @KtorExperimentalAPI
    fun opprettOppgaveIGosys(fremtidigOppgave: FremtidigOppgave) {
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(fremtidigOppgave.fnr)
        val gjelderUtland = (SYKEPENGER_UTLAND == behandlendeEnhet)
        runBlocking {
            oppgaveClient.opprettOppgave(
                sakId = fremtidigOppgave.saksId,
                journalpostId = fremtidigOppgave.journalpostId,
                tildeltEnhetsnr = behandlendeEnhet,
                aktoerId = fremtidigOppgave.aktørId,
                gjelderUtland = gjelderUtland
            )
        }
    }

    fun oppdater(oppgave: FremtidigOppgave) {
        log.info("Endrer oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${oppgave.tilstand.name}")
    }

    fun opprett(fremtidigOppgave: FremtidigOppgave) {
        utsattOppgaveDao.opprett(fremtidigOppgave)
    }

}

data class FremtidigOppgave(
    val id: Int = 0,
    val fnr: String,
    val saksId: String,
    val aktørId: String,
    val journalpostId: String,
    val arkivreferanse: String,
    val inntektsmeldingId: UUID,
    val tilstand: Tilstand = Tilstand.Ny,
    val timeout: LocalDateTime = LocalDateTime.now().plusHours(1)
)

enum class Tilstand {
    Ny, Utsatt, Forkastet, Opprettet
}

class OppgaveOppdatering(
    val id: UUID,
    val handling: Handling,
    val timeout: LocalDateTime?
)

enum class Handling {
    Utsett, Opprett, Forkast
}
