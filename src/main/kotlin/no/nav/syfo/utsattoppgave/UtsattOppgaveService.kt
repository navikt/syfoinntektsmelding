package no.nav.syfo.utsattoppgave

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.service.SYKEPENGER_UTLAND
import no.nav.syfo.util.Metrikk
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

@KtorExperimentalAPI
class UtsattOppgaveService(
    private val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val metrikk: Metrikk
) {

    val log = LoggerFactory.getLogger(UtsattOppgaveService::class.java)!!

    fun prosesser(oppdatering: OppgaveOppdatering) {
        val oppgave = utsattOppgaveDAO.finn(oppdatering.id.toString())
        if (oppgave == null) {
            metrikk.tellUtsattOppgave_Ukjent()
            log.warn("Mottok oppdatering på en ukjent oppgave")
            return
        }

        val gjelderSpeil = oppdatering.oppdateringstype == OppdateringstypeDTO.OpprettSpeilRelatert

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Utsett) {
            if (oppgave.timeout == null) {
                metrikk.tellUtsattOppgave_UtenDato()
            }
            oppdatering.timeout ?: error("Timeout på utsettelse mangler")
            oppgave.timeout = oppdatering.timeout
            oppgave.oppdatert = LocalDateTime.now()
            oppgave.speil = gjelderSpeil
            lagre(oppgave)
            metrikk.tellUtsattOppgave_Utsett()
            log.info("Oppdaterte timeout på inntektsmelding: ${oppgave.inntektsmeldingId} til ${oppdatering.timeout}")
            return
        }

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Forkast) {
            oppgave.oppdatert = LocalDateTime.now()
            lagre(oppgave.copy(tilstand = Tilstand.Forkastet, speil = gjelderSpeil))
            metrikk.tellUtsattOppgave_Forkast()
            log.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Forkastet.name}")
            return
        }

        if ((oppgave.tilstand == Tilstand.Utsatt || oppgave.tilstand == Tilstand.Forkastet) && oppdatering.handling == Handling.Opprett) {
            val resultat = opprettOppgaveIGosys(oppgave, oppgaveClient, utsattOppgaveDAO, behandlendeEnhetConsumer, gjelderSpeil)
            oppgave.oppdatert = LocalDateTime.now()
            lagre(oppgave.copy(tilstand = Tilstand.Opprettet, speil = gjelderSpeil))
            metrikk.tellUtsattOppgave_Opprett()
            log.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Opprettet.name} gosys oppgaveID: ${resultat.oppgaveId} duplikat? ${resultat.duplikat}")
            return
        }

        metrikk.tellUtsattOppgave_Irrelevant()
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
fun opprettOppgaveIGosys(
    utsattOppgave: UtsattOppgaveEntitet,
    oppgaveClient: OppgaveClient,
    utsattOppgaveDAO: UtsattOppgaveDAO,
    behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    speil: Boolean
): OppgaveResultat {
    val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(utsattOppgave.fnr, utsattOppgave.inntektsmeldingId)
    val gjelderUtland = (SYKEPENGER_UTLAND == behandlendeEnhet)
    val resultat = runBlocking {
        oppgaveClient.opprettOppgave(
            sakId = utsattOppgave.sakId,
            journalpostId = utsattOppgave.journalpostId,
            tildeltEnhetsnr = behandlendeEnhet,
            aktoerId = utsattOppgave.aktørId,
            gjelderUtland = gjelderUtland,
            gjelderSpeil = speil
        )
    }
    utsattOppgave.enhet = behandlendeEnhet
    utsattOppgave.gosysOppgaveId = resultat.oppgaveId.toString()
    utsattOppgaveDAO.lagre(utsattOppgave)
    return resultat
}

class OppgaveOppdatering(
    val id: UUID,
    val handling: Handling,
    val timeout: LocalDateTime?,
    val oppdateringstype: OppdateringstypeDTO
)

enum class Handling {
    Utsett, Opprett, Forkast
}
