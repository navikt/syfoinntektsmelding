package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.logger
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

    fun opprettOppgaveIGosys(
        utsattOppgave: UtsattOppgaveEntitet,
        speil: Boolean,
        imEntitet: InntektsmeldingEntitet,
    ): OppgaveResultat {
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

        utsattOppgave
            .apply {
                enhet = behandlendeEnhet
                gosysOppgaveId = resultat.oppgaveId.toString()
                utbetalingBruker = resultat.utbetalingBruker
            }
            .lagre()

        return resultat
    }

    fun prosesser(oppdatering: OppgaveOppdatering) {
        val oppgave = utsattOppgaveDAO.finn(oppdatering.id.toString())
        if (oppgave == null) {
            metrikk.tellUtsattOppgave_Ukjent()
            logger.warn("Mottok oppdatering på en ukjent oppgave")
            return
        }
        logger.info("Fant oppgave for inntektsmelding: ${oppgave.arkivreferanse} med tilstand: ${Tilstand.Forkastet.name}")
        val gjelderSpeil = oppdatering.oppdateringstype == OppdateringstypeDTO.OpprettSpeilRelatert

        return when {
            oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Utsett -> {
                oppdatering.timeout ?: error("Timeout på utsettelse mangler for inntektsmelding: ${oppgave.arkivreferanse}")

                oppgave
                    .apply {
                        timeout = oppdatering.timeout
                        oppdatert = LocalDateTime.now()
                        speil = gjelderSpeil
                    }
                    .lagre()

                metrikk.tellUtsattOppgave_Utsett()
                logger.info("Oppdaterte timeout på inntektsmelding: ${oppgave.arkivreferanse} til ${oppdatering.timeout}")
            }

            oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Forkast -> {
                oppgave
                    .apply {
                        oppdatert = LocalDateTime.now()
                    }
                    .copy(
                        tilstand = Tilstand.Forkastet,
                        speil = gjelderSpeil,
                    )
                    .lagre()

                metrikk.tellUtsattOppgave_Forkast()
                logger.info("Endret oppgave: ${oppgave.arkivreferanse} til tilstand: ${Tilstand.Forkastet.name}")
            }

            oppgave.tilstand in listOf(Tilstand.Utsatt, Tilstand.Forkastet) && oppdatering.handling == Handling.Opprett -> {
                val inntektsmeldingEntitet = inntektsmeldingRepository.findByArkivReferanse(oppgave.arkivreferanse)
                val resultat = opprettOppgaveIGosys(oppgave, gjelderSpeil, inntektsmeldingEntitet)

                oppgave
                    .apply {
                        oppdatert = LocalDateTime.now()
                    }
                    .copy(
                        tilstand = Tilstand.Opprettet,
                        speil = gjelderSpeil,
                    )
                    .lagre()

                metrikk.tellUtsattOppgave_Opprett()
                logger.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Opprettet.name} gosys oppgaveID: ${resultat.oppgaveId} duplikat? ${resultat.duplikat}")
            }

            else -> {
                metrikk.tellUtsattOppgave_Irrelevant()
                logger.info("Oppdatering på dokumentId: ${oppdatering.id} ikke relevant")
            }
        }
    }

    fun opprett(utsattOppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.opprett(utsattOppgave)
    }

    private fun UtsattOppgaveEntitet.lagre() {
        utsattOppgaveDAO.lagre(this)
    }
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
