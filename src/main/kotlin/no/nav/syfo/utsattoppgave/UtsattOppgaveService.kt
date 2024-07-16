package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.service.SYKEPENGER_UTLAND
import no.nav.syfo.util.Metrikk
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
    private val sikkerlogger = sikkerLogger()

    fun prosesser(oppdatering: OppgaveOppdatering) {
        val oppgave = utsattOppgaveDAO.finn(oppdatering.id.toString())
        if (oppgave == null) {
            metrikk.tellUtsattOppgave_Ukjent()
            logger.info("Mottok oppdatering på en ukjent oppgave for id: ${oppdatering.id}")
            return
        }
        logger.info("Fant oppgave for inntektsmelding: ${oppgave.arkivreferanse} med tilstand: ${oppgave.tilstand.name}")
        val gjelderSpeil = oppdatering.oppdateringstype.erSpeilRelatert()
        when (oppgave.tilstand to oppdatering.handling) {
            (Tilstand.Utsatt to Handling.Utsett) -> {
                oppdatering.timeout ?: error("Timeout på utsettelse mangler for inntektsmelding: ${oppgave.arkivreferanse}")
                oppgave.apply {
                    timeout = oppdatering.timeout
                    oppdatert = LocalDateTime.now()
                    speil = gjelderSpeil
                }
                lagre(oppgave)
                metrikk.tellUtsattOppgave_Utsett()
                logger.info("Oppdaterte timeout på inntektsmelding: ${oppgave.arkivreferanse} til ${oppdatering.timeout}")
            }
            (Tilstand.Utsatt to Handling.Forkast) -> {
                oppgave.oppdatert = LocalDateTime.now()
                lagre(oppgave.copy(tilstand = Tilstand.Forkastet, speil = gjelderSpeil))
                metrikk.tellUtsattOppgave_Forkast()
                logger.info("Endret oppgave: ${oppgave.arkivreferanse} til tilstand: ${Tilstand.Forkastet.name}")
            }
            (Tilstand.Utsatt to Handling.Opprett),
            (Tilstand.Forkastet to Handling.Opprett),
            -> {
                val inntektsmeldingEntitet = inntektsmeldingRepository.findByUuid(oppgave.inntektsmeldingId)
                if (inntektsmeldingEntitet != null) {
                    val inntektsmelding = om.readValue<Inntektsmelding>(inntektsmeldingEntitet.data!!)
                    val behandlendeEnhet =
                        behandlendeEnhetConsumer.hentBehandlendeEnhet(
                            oppgave.fnr,
                            oppgave.inntektsmeldingId,
                        )
                    val gjelderUtland = (SYKEPENGER_UTLAND == behandlendeEnhet)
                    logger.info("Fant enhet $behandlendeEnhet for ${oppgave.arkivreferanse}")
                    val behandlingsKategori = finnBehandlingsKategori(inntektsmelding, gjelderSpeil, gjelderUtland)
                    val resultat = opprettOppgave(oppgave, behandlingsKategori)
                    oppgave.oppdatert = LocalDateTime.now()
                    lagre(oppgave.copy(tilstand = Tilstand.Opprettet, speil = gjelderSpeil))
                    metrikk.tellUtsattOppgave_Opprett()
                    logger.info(
                        "Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Opprettet.name} gosys oppgaveID: ${resultat.oppgaveId} duplikat? ${resultat.duplikat}",
                    )
                } else {
                    sikkerlogger.error("Fant ikke inntektsmelding for ID '${oppgave.inntektsmeldingId}'.")
                }
            }
            else -> {
                metrikk.tellUtsattOppgave_Irrelevant()
                logger.info("Oppdatering på dokumentId: ${oppdatering.id} ikke relevant")
            }
        }
    }
    fun lagre(oppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.lagre(oppgave)
    }

    fun opprett(utsattOppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.opprett(utsattOppgave)
    }

    fun opprettOppgave(
        oppgave: UtsattOppgaveEntitet,
        behandlingsKategori: BehandlingsKategori
    ): OppgaveResultat = opprettOppgaveIGosys(oppgave, oppgaveClient, utsattOppgaveDAO, behandlingsKategori)
}

fun opprettOppgaveIGosys(
    utsattOppgave: UtsattOppgaveEntitet,
    oppgaveClient: OppgaveClient,
    utsattOppgaveDAO: UtsattOppgaveDAO,
    behandlingsKategori: BehandlingsKategori
): OppgaveResultat {
    val resultat =
        runBlocking {
            oppgaveClient.opprettOppgave(
                journalpostId = utsattOppgave.journalpostId,
                aktoerId = utsattOppgave.aktørId,
                behandlingsKategori = behandlingsKategori,
            )
        }
    utsattOppgave.gosysOppgaveId = resultat.oppgaveId.toString()
    utsattOppgave.utbetalingBruker = resultat.utbetalingBruker
    utsattOppgaveDAO.lagre(utsattOppgave)
    return resultat
}

class OppgaveOppdatering(
    val id: UUID,
    val handling: Handling,
    val timeout: LocalDateTime?,
    val oppdateringstype: OppdateringstypeDTO,
)

enum class Handling {
    Utsett,
    Opprett,
    Forkast,
}
