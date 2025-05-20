package no.nav.syfo.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class InntektsmeldingEntitet(
    val uuid: String,
    var aktorId: String,
    var journalpostId: String,
    var orgnummer: String? = null,
    var arbeidsgiverPrivat: String? = null,
    var behandlet: LocalDateTime? = LocalDateTime.now(),
    var data: String? = null,
) {
    var arbeidsgiverperioder: MutableList<ArbeidsgiverperiodeEntitet> = ArrayList()

    fun leggtilArbeidsgiverperiode(
        fom: LocalDate,
        tom: LocalDate,
    ) {
        val periode = ArbeidsgiverperiodeEntitet(fom = fom, tom = tom)
        periode.inntektsmelding = this
        arbeidsgiverperioder.add(periode)
    }
}
