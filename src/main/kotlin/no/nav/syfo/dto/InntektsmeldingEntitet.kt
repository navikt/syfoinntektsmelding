package no.nav.syfo.dto


import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class InntektsmeldingEntitet(
    var uuid: String = UUID.randomUUID().toString(),
    var aktorId: String,
    var sakId: String,
    var journalpostId: String,
    var orgnummer: String? = null,
    var arbeidsgiverPrivat: String? = null,
    var behandlet: LocalDateTime? = LocalDateTime.now(),
    var data: String? = null
) {
    val arbeidsgiverperioder: MutableList<ArbeidsgiverperiodeEntitet> = ArrayList()

    fun leggtilArbeidsgiverperiode(fom: LocalDate, tom: LocalDate) {
        val periode = ArbeidsgiverperiodeEntitet(fom = fom, tom = tom)
        periode.inntektsmelding = this
        arbeidsgiverperioder.add(periode)
    }
}
