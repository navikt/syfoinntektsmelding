package no.nav.syfo.prosesser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.behandling.BehandlingException
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.client.oppgave.OppgaveService
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.kafkamottak.InntektsmeldingConsumerException
import no.nav.syfo.util.Metrikk

/**
 * En bakgrunnsjobb som kan prosessere bakgrunnsjobber med inntektsmeldinger fra Joark
 */

class JoarkInntektsmeldingHendelseProsessor(
    private val om: ObjectMapper,
    private val metrikk: Metrikk,
    private val inntektsmeldingBehandler: InntektsmeldingBehandler,
    private val oppgaveService: OppgaveService,
) : BakgrunnsjobbProsesserer {
    private val logger = logger()
    private val sikkerlogger = sikkerLogger()

    companion object {
        const val JOB_TYPE = "joark-ny-inntektsmelding"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb): Unit =
        MdcUtils.withCallId {
            var arkivReferanse = "UKJENT"
            try {
                val journalpostDTO = om.readValue<InngaaendeJournalpostDTO>(jobb.data)
                sikkerlogger.info("Bruker InngaaendeJournalpostDTO: $journalpostDTO ")
                arkivReferanse = journalpostDTO.kanalReferanseId.ifEmpty { "UKJENT" }

                if (arkivReferanse == "UKJENT") {
                    throw IllegalArgumentException("Mottok inntektsmelding uten arkivreferanse")
                }

                logger.info("Bakgrunnsbehandler $arkivReferanse")
                if (jobb.forsoek >= jobb.maksAntallForsoek) {
                    "Jobb med arkivreferanse $arkivReferanse feilet permanent! Oppretter fordelingsoppgave".also {
                        logger.error(it)
                        sikkerlogger.error(it)
                    }
                    metrikk.tellRekjørerFeilet()
                    runBlocking {
                        opprettFordelingsoppgave(journalpostDTO.journalpostId.toString())
                    }
                    metrikk.tellOpprettFordelingsoppgave()
                    return@withCallId
                }
                inntektsmeldingBehandler.behandle(journalpostDTO.journalpostId.toString(), arkivReferanse)
            } catch (e: IllegalArgumentException) {
                metrikk.tellInntektsmeldingUtenArkivReferanse()
                throw InntektsmeldingConsumerException(e)
            } catch (e: BehandlingException) {
                "Feil ved behandling av inntektsmelding med arkivreferanse $arkivReferanse".also {
                    logger.error(it)
                    sikkerlogger.error(it, e)
                }
                metrikk.tellBehandlingsfeil(e.feiltype)
                throw InntektsmeldingConsumerException(e)
            } catch (e: Exception) {
                "Det skjedde en feil ved journalføring med arkivreferanse $arkivReferanse".also {
                    logger.error(it)
                    sikkerlogger.error(it, e)
                }
                metrikk.tellBehandlingsfeil(Feiltype.USPESIFISERT)
                throw InntektsmeldingConsumerException(e)
            }
        }

    private suspend fun opprettFordelingsoppgave(journalpostId: String) {
        oppgaveService.opprettFordelingsOppgave(journalpostId)
    }
}
