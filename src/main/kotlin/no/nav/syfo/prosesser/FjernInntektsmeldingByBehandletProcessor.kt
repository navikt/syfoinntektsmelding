package no.nav.syfo.prosesser

import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.repository.InntektsmeldingRepository
import java.time.LocalDate
import java.util.UUID

class FjernInntektsmeldingByBehandletProcessor(
    private val repository: InntektsmeldingRepository,
    private val lagringstidMåneder: Int
) : BakgrunnsjobbProsesserer {
    private val logger = this.logger()

    companion object { const val JOB_TYPE = "fjern-inntektsmelding-via-behnadling" }
    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        val konfigurertAntallMånederSiden = LocalDate.now().minusMonths(lagringstidMåneder.toLong()).atStartOfDay()
        logger.info("Sletter alle inntektsmeldinger før $konfigurertAntallMånederSiden")
        val antallSlettet = repository.deleteByBehandletBefore(konfigurertAntallMånederSiden)
        logger.info("Slettet $antallSlettet inntektsmeldinger")
    }

    data class JobbData(val id: UUID)
}
