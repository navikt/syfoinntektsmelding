package no.nav.syfo.utsattoppgave

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.SYKEPENGER_UTLAND
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@KtorExperimentalAPI
@Service
class UtsattOppgaveService(
    val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer
) {

    val log = log()

    @Scheduled(cron = "0 5,10,15,20,25,30,35,40,45,50,55 * * * *")
    fun opprettOppgaverForUtgåtte() {
        log.info("Sjekker for utgåtte oppgaver")
        utsattOppgaveDAO
            .finnAlleUtgåtteOppgaver()
            .forEach {
                try {
                    opprettOppgaveIGosys(it)
                    it.tilstand = Tilstand.Opprettet
                    lagre(it)
                    log.info("Oppgave opprettet i gosys for inntektsmelding: ${it.inntektsmeldingId}")
                } catch (e: OpprettOppgaveException) {
                    log.error("feil ved opprettelse av oppgave i gosys. InntektsmeldingId: ${it.inntektsmeldingId}")
                }
            }
    }

    fun prosesser(oppdatering: OppgaveOppdatering) {
        val oppgave = utsattOppgaveDAO.finn(oppdatering.id.toString())
        if (oppgave == null) {
            log.warn("Mottok oppdatering på en ukjent oppgave")
            return
        }

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Utsett) {
            oppdatering.timeout ?: error("Timeout på utsettelse mangler")
            oppgave.timeout = oppdatering.timeout
            lagre(oppgave)
            return
        }

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Forkast) {
            lagre(oppgave.copy(tilstand = Tilstand.Forkastet))
            return
        }

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Opprett) {
            opprettOppgaveIGosys(oppgave)
            lagre(oppgave.copy(tilstand = Tilstand.Opprettet))
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

    fun lagre(oppgave: UtsattOppgaveEntitet) {
        log.info("Endrer oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${oppgave.tilstand.name}")
        utsattOppgaveDAO.lagre(oppgave)
    }

    fun opprett(utsattOppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.opprett(utsattOppgave)
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
