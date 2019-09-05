package no.nav.syfo.mapping

import no.nav.inntektsmeldingkontrakt.*
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding

fun mapInntektsmelding(inntektsmelding: Inntektsmelding, arbeidstakerAktørId: String): no.nav.inntektsmeldingkontrakt.Inntektsmelding {
    return Inntektsmelding(
            inntektsmeldingId = inntektsmelding.id,
            arbeidstakerFnr = inntektsmelding.fnr,
            arbeidstakerAktorId = arbeidstakerAktørId,
            virksomhetsnummer = inntektsmelding.arbeidsgiverOrgnummer,
            arbeidsgiverFnr = inntektsmelding.arbeidsgiverPrivatFnr,
            arbeidsgiverAktorId = inntektsmelding.arbeidsgiverPrivatAktørId,
            arbeidsgivertype = mapArbeidsgivertype(inntektsmelding),
            arbeidsforholdId = inntektsmelding.arbeidsforholdId,
            arbeidsgiverperioder = mapArbeidsgiverperioder(inntektsmelding),
            beregnetInntekt = inntektsmelding.beregnetInntekt,
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
    check(!(inntektsmelding.arbeidsgiverOrgnummer.isNullOrEmpty() && inntektsmelding.arbeidsgiverPrivatFnr.isNullOrEmpty())) { "Inntektsmelding har hverken orgnummer eller fødselsnummer for arbeidsgiver" }
    check(!(!inntektsmelding.arbeidsgiverOrgnummer.isNullOrEmpty() && !inntektsmelding.arbeidsgiverPrivatFnr.isNullOrEmpty())) { "Inntektsmelding har både orgnummer og fødselsnummer for arbeidsgiver" }
    if (inntektsmelding.arbeidsgiverOrgnummer.isNullOrEmpty()) {
        return Arbeidsgivertype.PRIVAT
    }
    return Arbeidsgivertype.VIRKSOMHET
}

fun mapStatus(inntektsmelding: Inntektsmelding): Status {
    return Status.GYLDIG //TODO
}

fun mapGjenopptakelseAvNaturalytelser(inntektsmelding: Inntektsmelding): List<GjenopptakelseNaturalytelse> {
    return inntektsmelding.gjenopptakelserNaturalYtelse.map { go -> GjenopptakelseNaturalytelse(mapNaturalytelseType(go.naturalytelse), go.fom, go.beloepPrMnd) }
}

fun mapOpphørAvNaturalytelser(inntektsmelding: Inntektsmelding): List<OpphoerAvNaturalytelse> {
    return inntektsmelding.opphørAvNaturalYtelse.map { on -> OpphoerAvNaturalytelse(mapNaturalytelseType(on.naturalytelse), on.fom, on.beloepPrMnd) }
}

fun mapEndringIRefusjon(inntektsmelding: Inntektsmelding): List<EndringIRefusjon> {
    return inntektsmelding.endringerIRefusjon.map { er -> EndringIRefusjon(er.endringsdato, er.beloep) }
}

fun mapRefusjon(inntektsmelding: Inntektsmelding): Refusjon {
    return Refusjon(inntektsmelding.refusjon.beloepPrMnd, inntektsmelding.refusjon.opphoersdato)
}

fun mapNaturalytelseType(naturalytelseType: no.nav.syfo.domain.inntektsmelding.Naturalytelse?): Naturalytelse {
    return naturalytelseType?.let { n ->
        if (Naturalytelse.values().map { it.name }.contains(n.name)) Naturalytelse.valueOf(n.name) else Naturalytelse.annet
    }
            ?: Naturalytelse.annet
}