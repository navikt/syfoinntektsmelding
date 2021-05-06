package no.nav.syfo.prosesser

import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.syfo.repository.InntektsmeldingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class FjernInntektsmeldingByBehandletProcessor(
    private val repository: InntektsmeldingRepository,
    val lagringstidMåneder: Int
) : BakgrunnsjobbProsesserer {
    val log: Logger = LoggerFactory.getLogger(FjernInntektsmeldingByBehandletProcessor::class.java)
    companion object { val JOB_TYPE = "fjern-inntektsmelding-via-behnadling"}
    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        val konfigurertAntallMånederSiden = LocalDate.now().minusMonths(lagringstidMåneder.toLong()).atStartOfDay()
        log.info("Sletter alle inntektsmeldinger før $konfigurertAntallMånederSiden")
        val antallSlettet = repository.deleteByBehandletBefore(konfigurertAntallMånederSiden)
        log.info("Slettet $antallSlettet inntektsmeldinger")
    }

    data class JobbData(val id: UUID)
}
