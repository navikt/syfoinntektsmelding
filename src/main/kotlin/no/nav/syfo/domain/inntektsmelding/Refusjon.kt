package no.nav.syfo.domain.inntektsmelding

import java.math.BigDecimal
import java.time.LocalDate

data class Refusjon(
    val beloepPrMnd: BigDecimal? = null,
    val opphoersdato: LocalDate? = null
)
