package no.nav.syfo.simba

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.AvsenderSystem
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Kontaktinformasjon
import no.nav.syfo.domain.inntektsmelding.Refusjon
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

fun mapInntektsmelding(inntektsmeldingDokument: String): Inntektsmelding {
    return Inntektsmelding(
        "",
        "",
        "",
        "",
        "",
        "",
    "",
        "",
        JournalStatus.AVBRUTT,
        emptyList(),
        BigDecimal(12),
        Refusjon(BigDecimal(12)),
        emptyList(),
        emptyList(),
        emptyList(),
        Gyldighetsstatus.GYLDIG,
        "",
        emptyList(),
        LocalDate.now(),
        LocalDateTime.now(),
        "",
        "",
        "",
        AvsenderSystem("", ""),
        false,
        Kontaktinformasjon("", ""),
        LocalDateTime.now(),
        BigDecimal(100),
        ""
    )
}
