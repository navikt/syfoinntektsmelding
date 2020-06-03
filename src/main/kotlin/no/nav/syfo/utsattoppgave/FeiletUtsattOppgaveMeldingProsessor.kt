package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.bakgrunnsjobb.BakgrunnsjobbProsesserer
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * En bakgrunnsjobb som tar feilede meldinger ang utsatt oppgave og prøver å prosessere dem på nytt
 */
@Component
@KtorExperimentalAPI
class FeiletUtsattOppgaveMeldingProsessor(private val om: ObjectMapper, val oppgaveService: UtsattOppgaveService): BakgrunnsjobbProsesserer {

    companion object {
        val JOBB_TYPE = "feilet-utsatt-oppgave"
    }

    override fun prosesser(jobbData: String) {
        val utsattOppgaveOppdatering = om.readValue<UtsattOppgaveDTO>(jobbData)
        val oppdatering = OppgaveOppdatering(
            utsattOppgaveOppdatering.dokumentId,
            utsattOppgaveOppdatering.oppdateringstype.tilHandling(),
            utsattOppgaveOppdatering.timeout
        )

        oppgaveService.prosesser(oppdatering)
    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now().plusHours((forsoek * forsoek).toLong())
    }
}
