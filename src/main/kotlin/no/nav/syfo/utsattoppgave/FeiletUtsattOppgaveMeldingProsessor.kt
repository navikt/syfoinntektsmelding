package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger

/**
 * En bakgrunnsjobb som tar feilede meldinger ang utsatt oppgave og prøver å prosessere dem på nytt
 */

class FeiletUtsattOppgaveMeldingProsessor(
    private val om: ObjectMapper,
    val oppgaveService: UtsattOppgaveService
) :
    BakgrunnsjobbProsesserer {
    private val logger = this.logger()

    override val type: String get() = JOB_TYPE
    companion object {
        const val JOB_TYPE = "feilet-utsatt-oppgave"
    }

    override fun prosesser(jobb: Bakgrunnsjobb) {
        val utsattOppgaveOppdatering = om.readValue<UtsattOppgaveDTO>(jobb.data)
        val oppdatering = OppgaveOppdatering(
            utsattOppgaveOppdatering.dokumentId,
            utsattOppgaveOppdatering.oppdateringstype.tilHandling(),
            utsattOppgaveOppdatering.timeout,
            utsattOppgaveOppdatering.oppdateringstype
        )

        MdcUtils.withCallId {
            logger.info("Prosesserer inntekstmelding " + oppdatering.id)
            oppgaveService.prosesser(oppdatering)
        }
    }
}
