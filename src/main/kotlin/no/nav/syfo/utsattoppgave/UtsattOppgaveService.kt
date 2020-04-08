package no.nav.syfo.utsattoppgave

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.SYKEPENGER_UTLAND
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.helpers.log
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@KtorExperimentalAPI
@Service
class UtsattOppgaveService(
    val utsattOppgaveDao: UtsattOppgaveDao,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer
) {
    fun prosesser(oppdatering: OppgaveOppdatering) {
        val oppgave = utsattOppgaveDao.finn(oppdatering.id.toString())
        if (oppgave == null) {
            log.warn("Mottok oppdatering på en ukjent oppgave")
            return
        }

        if ((oppgave.tilstand == Tilstand.Ny || oppgave.tilstand == Tilstand.Utsatt) && oppdatering.handling == Handling.Utsett) {
            oppdatering.timeout ?: error("Timeout på utsettelse mangler")
            oppgave.tilstand = Tilstand.Utsatt
            oppgave.timeout = oppdatering.timeout
            oppdater(oppgave)
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
    fun opprettOppgaveIGosys(utsattOppgave: UtsattOppgaveEntitet) {
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(utsattOppgave.fnr)
        val gjelderUtland = (SYKEPENGER_UTLAND == behandlendeEnhet)
        runBlocking {
            oppgaveClient.opprettOppgave(
                sakId = utsattOppgave.sakId,
                journalpostId = utsattOppgave.journalpostId,
                tildeltEnhetsnr = behandlendeEnhet,
                aktoerId = utsattOppgave.aktørId,
                gjelderUtland = gjelderUtland
            )
        }
    }

    fun oppdater(oppgave: UtsattOppgaveEntitet) {
        log.info("Endrer oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${oppgave.tilstand.name}")
        utsattOppgaveDao.save(oppgave)
    }

    fun opprett(utsattOppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDao.opprett(utsattOppgave)
    }
}

class OppgaveOppdatering(
    val id: UUID,
    val handling: Handling,
    val timeout: LocalDateTime?
)

enum class Handling {
    Utsett, Opprett, Forkast
}
