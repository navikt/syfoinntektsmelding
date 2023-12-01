package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.service.SYKEPENGER_UTLAND
import no.nav.syfo.util.Metrikk
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class UtsattOppgaveService(
    private val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val om: ObjectMapper,
    private val metrikk: Metrikk
) {

    private val logger = this.logger()

    fun prosesser(oppdatering: OppgaveOppdatering) {
        val oppgave = utsattOppgaveDAO.finn(oppdatering.id.toString())
        if (oppgave == null) {
            metrikk.tellUtsattOppgave_Ukjent()
            logger.info("Mottok oppdatering på en ukjent oppgave for id: ${oppdatering.id}")
            return
        }
        logger.info("Fant oppgave for inntektsmelding: ${oppgave.arkivreferanse} med tilstand: ${oppgave.tilstand.name}")
        val gjelderSpeil = oppdatering.oppdateringstype.erSpeilRelatert()

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == no.nav.syfo.utsattoppgave.Handling.Utsett) {
            if (oppgave.timeout == null) {
                metrikk.tellUtsattOppgave_UtenDato()
            }
            oppdatering.timeout ?: error("Timeout på utsettelse mangler for inntektsmelding: ${oppgave.arkivreferanse}")
            oppgave.timeout = oppdatering.timeout
            oppgave.oppdatert = LocalDateTime.now()
            oppgave.speil = gjelderSpeil
            lagre(oppgave)
            metrikk.tellUtsattOppgave_Utsett()
            logger.info("Oppdaterte timeout på inntektsmelding: ${oppgave.arkivreferanse} til ${oppdatering.timeout}")
            return
        }

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == no.nav.syfo.utsattoppgave.Handling.Forkast) {
            oppgave.oppdatert = LocalDateTime.now()
            lagre(oppgave.copy(tilstand = Tilstand.Forkastet, speil = gjelderSpeil))
            metrikk.tellUtsattOppgave_Forkast()
            logger.info("Endret oppgave: ${oppgave.arkivreferanse} til tilstand: ${Tilstand.Forkastet.name}")
            return
        }

        if ((oppgave.tilstand == Tilstand.Utsatt || oppgave.tilstand == Tilstand.Forkastet) && oppdatering.handling == Handling.Opprett) {
            val inntektsmeldingEntitet = inntektsmeldingRepository.findByArkivReferanse(oppgave.arkivreferanse)
            val resultat = opprettOppgaveIGosys(oppgave, oppgaveClient, utsattOppgaveDAO, behandlendeEnhetConsumer, gjelderSpeil, inntektsmeldingEntitet, om)
            oppgave.oppdatert = LocalDateTime.now()
            lagre(oppgave.copy(tilstand = Tilstand.Opprettet, speil = gjelderSpeil))
            metrikk.tellUtsattOppgave_Opprett()
            logger.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Opprettet.name} gosys oppgaveID: ${resultat.oppgaveId} duplikat? ${resultat.duplikat}")
            return
        }

        metrikk.tellUtsattOppgave_Irrelevant()
        logger.info("Oppdatering på dokumentId: ${oppdatering.id} ikke relevant")
    }

    fun lagre(oppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.lagre(oppgave)
    }

    fun opprett(utsattOppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.opprett(utsattOppgave)
    }
}

fun opprettOppgaveIGosys(
    utsattOppgave: UtsattOppgaveEntitet,
    oppgaveClient: OppgaveClient,
    utsattOppgaveDAO: UtsattOppgaveDAO,
    behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    speil: Boolean,
    imEntitet: InntektsmeldingEntitet,
    om: ObjectMapper
): OppgaveResultat {
    val logger = LoggerFactory.getLogger(UtsattOppgaveService::class.java)!!
    val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(
        utsattOppgave.fnr,
        utsattOppgave.inntektsmeldingId
    )
    val gjelderUtland = (SYKEPENGER_UTLAND == behandlendeEnhet)
    val inntektsmelding = om.readValue<Inntektsmelding>(imEntitet.data!!)
    val behandlingsTema = finnBehandlingsTema(inntektsmelding)
    logger.info("Fant enhet $behandlendeEnhet for ${utsattOppgave.arkivreferanse}")
    val resultat = runBlocking {
        oppgaveClient.opprettOppgave(
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
    utsattOppgave.utbetalingBruker = resultat.utbetalingBruker
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
