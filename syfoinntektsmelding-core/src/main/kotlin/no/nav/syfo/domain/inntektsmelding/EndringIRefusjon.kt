package no.nav.syfo.domain.inntektsmelding

import java.math.BigDecimal
import java.time.LocalDate

data class EndringIRefusjon(
        val endringsdato: LocalDate? = null,
        val beloep: BigDecimal? = null
)