package no.nav.syfo.utsattoppgave

import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.SYKEPENGER_UTLAND
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@KtorExperimentalAPI
class UtsattOppgaveService(
    private val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer
) {

    val log = LoggerFactory.getLogger(UtsattOppgaveService::class.java)!!

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
            log.info("Oppdaterte timeout på inntektsmelding: ${oppgave.inntektsmeldingId} til ${oppdatering.timeout}")
            return
        }

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Forkast) {
            lagre(oppgave.copy(tilstand = Tilstand.Forkastet))
            log.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Forkastet.name}")
            return
        }

        if ((oppgave.tilstand == Tilstand.Utsatt || oppgave.tilstand == Tilstand.Forkastet) && oppdatering.handling == Handling.Opprett) {
            opprettOppgaveIGosys(oppgave, oppgaveClient, utsattOppgaveDAO, behandlendeEnhetConsumer)
            lagre(oppgave.copy(tilstand = Tilstand.Opprettet))
            log.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Opprettet.name}")
            return
        }

        log.info("Oppdatering på dokumentId: ${oppdatering.id} ikke relevant")
    }



    fun lagre(oppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.lagre(oppgave)
    }

    fun opprett(utsattOppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.opprett(utsattOppgave)
    }
}

@KtorExperimentalAPI
fun opprettOppgaveIGosys(utsattOppgave: UtsattOppgaveEntitet,
                         oppgaveClient: OppgaveClient,
                         utsattOppgaveDAO: UtsattOppgaveDAO,
                         behandlendeEnhetConsumer: BehandlendeEnhetConsumer) {
    val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(utsattOppgave.fnr, utsattOppgave.inntektsmeldingId)
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
    utsattOppgave.enhet = behandlendeEnhet;
    utsattOppgaveDAO.lagre(utsattOppgave);
}

class OppgaveOppdatering(
    val id: UUID,
    val handling: Handling,
    val timeout: LocalDateTime?
)

enum class Handling {
    Utsett, Opprett, Forkast
}
