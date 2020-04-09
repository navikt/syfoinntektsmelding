package no.nav.syfo.mapping

import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.Refusjon
import no.nav.syfo.dto.ArbeidsgiverperiodeEntitet
import no.nav.syfo.dto.InntektsmeldingEntitet
import java.time.LocalDate


fun toInntektsmeldingEntitet(inntektsmelding : Inntektsmelding) : InntektsmeldingEntitet {
    val entitet = InntektsmeldingEntitet(
            aktorId = inntektsmelding.aktorId ?: "",
            sakId = inntektsmelding.sakId ?: "",
            journalpostId = inntektsmelding.journalpostId,
            arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivatFnr,
            orgnummer = inntektsmelding.arbeidsgiverOrgnummer,
            behandlet = inntektsmelding.mottattDato
    )
    inntektsmelding.arbeidsgiverperioder.forEach { p -> entitet.leggtilArbeidsgiverperiode(p.fom, p.tom) }
    return entitet
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
            arsakTilInnsending = "?", // TODO - Dette feltet må populeres fra database
            journalStatus = JournalStatus.MIDLERTIDIG,
            arbeidsgiverperioder = mapPerioder(inntektsmeldingEntitet.arbeidsgiverperioder),
            beregnetInntekt = null,
            refusjon = Refusjon(),
            endringerIRefusjon = ArrayList(),
            opphørAvNaturalYtelse = ArrayList(),
            gjenopptakelserNaturalYtelse = ArrayList(),
            gyldighetsStatus = Gyldighetsstatus.GYLDIG,
            arkivRefereranse = "???", // TODO - Dette feltet må populeres fra database
            feriePerioder = ArrayList(),
            førsteFraværsdag = LocalDate.now(),
            mottattDato = inntektsmeldingEntitet.behandlet!!
    )
}

fun mapPerioder(perioder : List<ArbeidsgiverperiodeEntitet>): List<Periode> {
    return perioder.map{ p -> mapPeriode(p) }
}

fun mapPeriode(p : ArbeidsgiverperiodeEntitet) : Periode{
    return Periode(p.fom, p.tom)
}
