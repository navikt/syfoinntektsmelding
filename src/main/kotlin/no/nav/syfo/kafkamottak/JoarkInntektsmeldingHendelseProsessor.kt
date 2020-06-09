package no.nav.syfo.kafkamottak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.syfo.behandling.BehandlingException
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.mq.InntektsmeldingConsumerException
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.repository.FeiletService
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.Metrikk
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException
import java.time.LocalDateTime

/**
 * En bakgrunnsjobb som kan prosessere bakgrunnsjobber med inntektsmeldinger fra Joark
 */
@Component
@KtorExperimentalAPI
class JoarkInntektsmeldingHendelseProsessor(
    private val om: ObjectMapper,
    private val metrikk: Metrikk,
    private val inntektsmeldingBehandler: InntektsmeldingBehandler,
    private val feiletService: FeiletService,
    private val oppgaveClient: OppgaveClient): BakgrunnsjobbProsesserer {
    private val log = log()

    companion object {
        val JOBB_TYPE = "joark-ny-inntektsmelding"
    }

    override fun prosesser(jobbOpprettet: LocalDateTime, forsoek: Int, jobbData: String) {
        var arkivReferanse = "UKJENT"
        try {

            val journalpostDTO = om.readValue<InngaaendeJournalpostDTO>(jobbData)
            MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, MDCOperations.generateCallId())

            arkivReferanse = if (journalpostDTO.kanalReferanseId.isEmpty()) "UKJENT" else journalpostDTO.kanalReferanseId

            if (arkivReferanse == "UKJENT") {
                throw IllegalArgumentException("Mottok inntektsmelding uten arkivreferanse")
            }

            log.info("Bakgrunnsbehandler $arkivReferanse")
            val historikk = feiletService.finnHistorikk(arkivReferanse)

            if (historikk.feiletList.isNotEmpty()){
                metrikk.tellRekjørerFeilet()
            }

            if (historikk.skalArkiveresForDato()) {
                runBlocking {
                    opprettFordelingsoppgave(journalpostDTO.journalpostId.toString())
                }
                metrikk.tellOpprettFordelingsoppgave()
                return
            }

            inntektsmeldingBehandler.behandle(journalpostDTO.journalpostId.toString(), arkivReferanse)

        } catch(e: IllegalArgumentException) {
            metrikk.tellInntektsmeldingUtenArkivReferanse()
            throw InntektsmeldingConsumerException(arkivReferanse, e, Feiltype.INNGÅENDE_MANGLER_KANALREFERANSE)
        } catch (e: BehandlingException) {
            log.error("Feil ved behandling av inntektsmelding med arkivreferanse $arkivReferanse", e)
            metrikk.tellBehandlingsfeil(e.feiltype)
            lagreFeilet(arkivReferanse, e.feiltype)
            throw InntektsmeldingConsumerException(arkivReferanse, e, e.feiltype)
        } catch (e: Exception) {
            log.error("Det skjedde en feil ved journalføring med arkivreferanse $arkivReferanse", e)
            metrikk.tellBehandlingsfeil(Feiltype.USPESIFISERT)
            lagreFeilet(arkivReferanse, Feiltype.USPESIFISERT)
            throw InntektsmeldingConsumerException(arkivReferanse, e, Feiltype.USPESIFISERT)
        } finally {
            MDCOperations.remove(MDCOperations.MDC_CALL_ID)
        }
    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now().plusHours((forsoek * forsoek).toLong())
    }

    suspend fun opprettFordelingsoppgave(journalpostId: String): Boolean {
        oppgaveClient.opprettFordelingsOppgave(journalpostId)
        return true
    }

    fun lagreFeilet(arkivReferanse: String, feiltype: Feiltype) {
        try {
            feiletService.lagreFeilet(arkivReferanse, feiltype)
        } catch (e: Exception) {
            metrikk.tellLagreFeiletMislykkes();
        }
    }
}
