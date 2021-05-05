package no.nav.syfo.dto

import java.time.LocalDate
import java.util.*

data class ArbeidsgiverperiodeEntitet (
    var uuid: String = UUID.randomUUID().toString(),
    var inntektsmelding : InntektsmeldingEntitet? = null,
    var fom: LocalDate,
    var tom: LocalDate,
    var innteksmelding_uuid : String = ""
)
