package no.nav.syfo.domain.inntektsmelding

import java.math.BigDecimal
import java.time.LocalDate

data class GjenopptakelseNaturalytelse(
    val naturalytelse: Naturalytelse? = null,
    val fom: LocalDate? = null,
    val beloepPrMnd: BigDecimal? = null,
)
