package no.nav.syfo.dto


import no.nav.syfo.repository.ArbeidsgiverperiodeRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

data class InntektsmeldingEntitet(
    val uuid: String = UUID.randomUUID().toString(),
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

class ArbeidsgiverperiodeRepUtil(val rep: ArbeidsgiverperiodeRepository) {
    fun lagreArbeidsPeriode(periode: ArbeidsgiverperiodeEntitet) {
        rep.lagreData(periode)
    }
}
