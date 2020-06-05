package no.nav.syfo.kafkamottak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.bakgrunnsjobb.BakgrunnsjobbProsesserer
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * En bakgrunnsjobb som kan prosessere bakgrunnsjobber med inntektsmeldinger fra Joark
 */
@Component
@KtorExperimentalAPI
class JoarkInntektsmeldingHendelseProsessor(private val om: ObjectMapper): BakgrunnsjobbProsesserer {

    companion object {
        val JOBB_TYPE = "joark-ny-inntektsmelding"
    }

    override fun prosesser(jobbData: String) {
        //TODO: Flytt behandlingslogikken for inntektsmeldinger hit



    }

    override fun nesteForsoek(forsoek: Int, forrigeForsoek: LocalDateTime): LocalDateTime {
        return LocalDateTime.now().plusHours((forsoek * forsoek).toLong())
    }
}
