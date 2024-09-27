package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.client.OppgaveService
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.util.Metrikk
import java.time.LocalDateTime
import java.util.UUID

class UtsattOppgaveService(
    private val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveService: OppgaveService,
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
            (Tilstand.Forkastet to Handling.Opprett) -> {
                inntektsmeldingRepository.hentInntektsmelding(oppgave, om)
                    .onSuccess { inntektsmelding ->
                        val gjelderUtland = behandlendeEnhetConsumer.gjelderUtland(oppgave)
                        val behandlingsKategori = utledBehandlingsKategori(oppgave, inntektsmelding, gjelderUtland)
                        if (BehandlingsKategori.IKKE_FRAVAER != behandlingsKategori) {
                            val resultat = opprettOppgave(oppgave, behandlingsKategori)
                            oppgave.oppdatert = LocalDateTime.now()
                            lagre(oppgave.copy(tilstand = Tilstand.Opprettet, speil = gjelderSpeil))
                            metrikk.tellUtsattOppgave_Opprett()
                            logger.info(
                                "Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Opprettet.name} gosys oppgaveID: ${resultat.oppgaveId} duplikat? ${resultat.duplikat}",
                            )
                        } else {
                            if (oppgave.tilstand == Tilstand.Utsatt) {
                                oppgave.oppdatert = LocalDateTime.now()
                                lagre(oppgave.copy(tilstand = Tilstand.Forkastet, speil = gjelderSpeil))
                                sikkerlogger.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Forkastet.name} pga behandlingskategori: $behandlingsKategori")
                            }
                            logger.info("Oppgave blir ikke opprettet for inntektsmeldingId: ${oppgave.inntektsmeldingId}")
                            sikkerlogger.info("ppgave blir ikke opprettet for inntektsmeldingId: ${oppgave.inntektsmeldingId}, har behandlingskategori: $behandlingsKategori")
                        }
                    }
                    .onFailure { sikkerlogger.error(it.message, it) }
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
    ): OppgaveResultat = opprettOppgaveIGosys(oppgave, oppgaveService, utsattOppgaveDAO, behandlingsKategori)
}

fun InntektsmeldingRepository.hentInntektsmelding(oppgave: UtsattOppgaveEntitet, om: ObjectMapper): Result<Inntektsmelding> {
    val inntektsmeldingData = this.findByUuid(oppgave.inntektsmeldingId)?.data
    return if (inntektsmeldingData != null) {
        Result.success(om.readValue<Inntektsmelding>(inntektsmeldingData))
    } else {
        Result.failure(Exception("Fant ikke inntektsmelding for ID '${oppgave.inntektsmeldingId}'."))
    }
}

fun opprettOppgaveIGosys(
    utsattOppgave: UtsattOppgaveEntitet,
    oppgaveService: OppgaveService,
    utsattOppgaveDAO: UtsattOppgaveDAO,
    behandlingsKategori: BehandlingsKategori
): OppgaveResultat {
    val resultat =
        runBlocking {
            oppgaveService.opprettOppgave(
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
    val oppdateringstype: OppdateringstypeDTO
)

enum class Handling {
    Utsett, Opprett, Forkast
}
