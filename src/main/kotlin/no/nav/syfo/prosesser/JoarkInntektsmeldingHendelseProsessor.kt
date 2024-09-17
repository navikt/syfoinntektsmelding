package no.nav.syfo.prosesser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.behandling.BehandlingException
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.kafkamottak.InntektsmeldingConsumerException
import no.nav.syfo.util.Metrikk
import org.slf4j.LoggerFactory

/**
 * En bakgrunnsjobb som kan prosessere bakgrunnsjobber med inntektsmeldinger fra Joark
 */

class JoarkInntektsmeldingHendelseProsessor(
    private val om: ObjectMapper,
    private val metrikk: Metrikk,
    private val inntektsmeldingBehandler: InntektsmeldingBehandler,
    private val oppgaveClient: OppgaveClient
) : BakgrunnsjobbProsesserer {
    private val logger = this.logger()
    private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")

    companion object {
        const val JOB_TYPE = "joark-ny-inntektsmelding"
    }

    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb): Unit = MdcUtils.withCallId {
        var arkivReferanse = "UKJENT"
        try {
            val journalpostDTO = om.readValue<InngaaendeJournalpostDTO>(jobb.data)
            sikkerlogger.info("Bruker InngaaendeJournalpostDTO: $journalpostDTO ")
            arkivReferanse = if (journalpostDTO.kanalReferanseId.isEmpty()) "UKJENT" else journalpostDTO.kanalReferanseId

            if (arkivReferanse == "UKJENT") {
                throw IllegalArgumentException("Mottok inntektsmelding uten arkivreferanse")
            }

            logger.info("Bakgrunnsbehandler $arkivReferanse")
            if (jobb.forsoek >= jobb.maksAntallForsoek) {
                sikkerlogger.error("Jobb med arkivreferanse $arkivReferanse feilet permanent! Oppretter fordelingsoppgave")
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
            sikkerlogger.error("Feil ved behandling av inntektsmelding med arkivreferanse $arkivReferanse", e)
            metrikk.tellBehandlingsfeil(e.feiltype)
            throw InntektsmeldingConsumerException(e)
        } catch (e: Exception) {
            sikkerlogger.error("Det skjedde en feil ved journalføring med arkivreferanse $arkivReferanse", e)
            metrikk.tellBehandlingsfeil(Feiltype.USPESIFISERT)
            throw InntektsmeldingConsumerException(e)
        }
    }

    private suspend fun opprettFordelingsoppgave(journalpostId: String) {
        oppgaveClient.opprettFordelingsOppgave(journalpostId)
    }
}
