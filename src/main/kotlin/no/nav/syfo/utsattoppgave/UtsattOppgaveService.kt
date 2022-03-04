package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.InntektsmeldingRepository
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
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val om: ObjectMapper,
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

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == no.nav.syfo.utsattoppgave.Handling.Utsett) {
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

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == no.nav.syfo.utsattoppgave.Handling.Forkast) {
            oppgave.oppdatert = LocalDateTime.now()
            lagre(oppgave.copy(tilstand = Tilstand.Forkastet, speil = gjelderSpeil))
            metrikk.tellUtsattOppgave_Forkast()
            log.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Forkastet.name}")
            return
        }

        if ((oppgave.tilstand == Tilstand.Utsatt || oppgave.tilstand == Tilstand.Forkastet) && oppdatering.handling == Handling.Opprett) {
            val resultat = opprettOppgaveIGosys(oppgave, oppgaveClient, utsattOppgaveDAO, behandlendeEnhetConsumer, gjelderSpeil, inntektsmeldingRepository, om)
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
    speil: Boolean,
    inntektsmeldingRepository: InntektsmeldingRepository,
    om: ObjectMapper
): OppgaveResultat {
    val log = LoggerFactory.getLogger(UtsattOppgaveService::class.java)!!
    val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(utsattOppgave.fnr, utsattOppgave.inntektsmeldingId)
    val gjelderUtland = (SYKEPENGER_UTLAND == behandlendeEnhet)
    val imEntitet = inntektsmeldingRepository.findByArkivReferanse(utsattOppgave.arkivreferanse)
    val inntektsmelding = om.readValue<Inntektsmelding>(imEntitet.data!!)
    val behandlingsTema = finnBehandlingsTema(inntektsmelding)
    log.info("Fant enhet $behandlendeEnhet for ${utsattOppgave.arkivreferanse}")
    val resultat = runBlocking {
        oppgaveClient.opprettOppgave(
            sakId = utsattOppgave.sakId,
            journalpostId = utsattOppgave.journalpostId,
            tildeltEnhetsnr = null,
            aktoerId = utsattOppgave.aktørId,
            gjelderUtland = gjelderUtland,
            gjelderSpeil = speil,
            tema = behandlingsTema
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
