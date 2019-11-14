package no.nav.syfo.mapping

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.dto.ArbeidsgiverperiodeEntitet
import no.nav.syfo.dto.InntektsmeldingEntitet
import java.time.LocalDate


fun toInntektsmeldingDTO(inntektsmelding : Inntektsmelding) : InntektsmeldingEntitet {
    val dto = InntektsmeldingEntitet(
            aktorId = inntektsmelding.aktorId ?: "",
            sakId = inntektsmelding.sakId ?: "",
            journalpostId = inntektsmelding.journalpostId,
            arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivatFnr,
            orgnummer = inntektsmelding.arbeidsgiverOrgnummer,
            behandlet = inntektsmelding.mottattDato
    )
    inntektsmelding.arbeidsgiverperioder.forEach { p -> dto.leggtilArbeidsgiverperiode(p.fom, p.tom) }
    return dto
}

fun toInntektsmelding(inntektsmeldingEntitet: InntektsmeldingEntitet) : Inntektsmelding {
    return Inntektsmelding(
            id = inntektsmeldingEntitet.uuid!!,
            fnr = inntektsmeldingEntitet.arbeidsgiverPrivat ?: "",
            sakId = inntektsmeldingEntitet.sakId,
            aktorId = inntektsmeldingEntitet.aktorId,
            arbeidsgiverOrgnummer = inntektsmeldingEntitet.orgnummer,
            arbeidsgiverPrivatFnr = inntektsmeldingEntitet.arbeidsgiverPrivat,
            arbeidsforholdId = null,
            journalpostId = inntektsmeldingEntitet.journalpostId,
            arsakTilInnsending = "?",
            journalStatus = JournalStatus.MIDLERTIDIG,
            arbeidsgiverperioder = mapPerioder(inntektsmeldingEntitet.arbeidsgiverperioder),
            beregnetInntekt = null,
            refusjon = Refusjon(),
            endringerIRefusjon = ArrayList(),
            opphørAvNaturalYtelse = ArrayList(),
            gjenopptakelserNaturalYtelse = ArrayList(),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = "???",
            feriePerioder = ArrayList(),
            førsteFraværsdag = LocalDate.now(),
            mottattDato = inntektsmeldingEntitet.behandlet!!
    )
}

fun mapPerioder(perioder : List<ArbeidsgiverperiodeEntitet>): List<Periode> {
    return perioder.map{ p -> mapPeriode(p) }
}

// TODO Fjern null sjekken Morten
fun mapPeriode(p : ArbeidsgiverperiodeEntitet) : Periode{
    return Periode(p.fom!!, p.tom!!)
}
