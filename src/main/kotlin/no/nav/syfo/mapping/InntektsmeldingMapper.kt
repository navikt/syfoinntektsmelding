package no.nav.syfo.mapping

import no.nav.inntektsmeldingkontrakt.*
import no.nav.syfo.domain.Inntektsmelding

fun mapInntektsmelding(inntektsmelding: Inntektsmelding, arbeidstakerAktørId: String): no.nav.inntektsmeldingkontrakt.Inntektsmelding {
    return Inntektsmelding(
            inntektsmeldingId = "", //TODO
            arbeidstakerFnr = inntektsmelding.fnr,
            arbeidstakerAktorId = arbeidstakerAktørId,
            virksomhetsnummer = inntektsmelding.arbeidsgiverOrgnummer,
            arbeidsgiverFnr = inntektsmelding.arbeidsgiverPrivat,
            arbeidsgiverAktorId = "", //TODO
            arbeidsgivertype = mapArbeidsgivertype(inntektsmelding),
            arbeidsforholdId = inntektsmelding.arbeidsforholdId,
            arbeidsgiverperioder = mapArbeidsgiverperioder(inntektsmelding),
            beregnetInntekt = null, //TODO
            refusjon = mapRefusjon(inntektsmelding),
            endringIRefusjoner = mapEndringIRefusjon(inntektsmelding),
            opphoerAvNaturalytelser = mapOpphørAvNaturalytelser(inntektsmelding),
            gjenopptakelseNaturalytelser = mapGjenopptakelseAvNaturalytelser(inntektsmelding),
            status = mapStatus(inntektsmelding)

    )
}

fun mapArbeidsgiverperioder(inntektsmelding: Inntektsmelding): List<Periode> {
    return inntektsmelding.arbeidsgiverperioder.map { p -> Periode(p.fom, p.tom) }
}

fun mapArbeidsgivertype(inntektsmelding: Inntektsmelding): Arbeidsgivertype {
    check(!(inntektsmelding.arbeidsgiverOrgnummer.isNullOrEmpty() && inntektsmelding.arbeidsgiverPrivat.isNullOrEmpty())) { "Inntektsmelding har hverken orgnummer eller fødselsnummer for arbeidsgiver" }
    check(!(!inntektsmelding.arbeidsgiverOrgnummer.isNullOrEmpty() && !inntektsmelding.arbeidsgiverPrivat.isNullOrEmpty())) { "Inntektsmelding har både orgnummer og fødselsnummer for arbeidsgiver" }
    if (inntektsmelding.arbeidsgiverOrgnummer.isNullOrEmpty()) {
        return Arbeidsgivertype.PRIVAT
    }
    return Arbeidsgivertype.VIRKSOMHET
}

fun mapStatus(inntektsmelding: Inntektsmelding): Status {
    return Status.GYLDIG //TODO
}

fun mapGjenopptakelseAvNaturalytelser(inntektsmelding: Inntektsmelding): List<GjenopptakelseNaturalytelse> {
    return emptyList() //TODO
}

fun mapOpphørAvNaturalytelser(inntektsmelding: Inntektsmelding): List<OpphoerAvNaturalytelse> {
    return emptyList() //TODO
}

fun mapEndringIRefusjon(inntektsmelding: Inntektsmelding): List<EndringIRefusjon> {
    return emptyList() //TODO
}

fun mapRefusjon(inntektsmelding: Inntektsmelding): Refusjon {
    return Refusjon() //TODO
}
