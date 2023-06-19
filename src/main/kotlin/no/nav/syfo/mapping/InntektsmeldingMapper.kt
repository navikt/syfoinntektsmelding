package no.nav.syfo.mapping

import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.AvsenderSystem
import no.nav.inntektsmeldingkontrakt.EndringIRefusjon
import no.nav.inntektsmeldingkontrakt.GjenopptakelseNaturalytelse
import no.nav.inntektsmeldingkontrakt.Naturalytelse
import no.nav.inntektsmeldingkontrakt.OpphoerAvNaturalytelse
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding

fun mapInntektsmeldingKontrakt(
    inntektsmelding: Inntektsmelding,
    arbeidstakerAktørId: String,
    gyldighetsstatus: Gyldighetsstatus,
    arkivreferanse: String,
    uuid: String
): no.nav.inntektsmeldingkontrakt.Inntektsmelding {
    return no.nav.inntektsmeldingkontrakt.Inntektsmelding(
        inntektsmeldingId = uuid,
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
        status = mapStatus(gyldighetsstatus),
        arkivreferanse = arkivreferanse,
        ferieperioder = mapFerieperioder(inntektsmelding),
        foersteFravaersdag = inntektsmelding.førsteFraværsdag,
        mottattDato = inntektsmelding.mottattDato,
        begrunnelseForReduksjonEllerIkkeUtbetalt = inntektsmelding.begrunnelseRedusert,
        naerRelasjon = inntektsmelding.nærRelasjon,
        avsenderSystem = mapAvsenderSystem(inntektsmelding.avsenderSystem)
    )
}

fun mapFerieperioder(inntektsmelding: Inntektsmelding): List<Periode> {
    return inntektsmelding.feriePerioder.map { p -> Periode(p.fom, p.tom) }
}

fun mapStatus(status: Gyldighetsstatus): Status {
    if (status == Gyldighetsstatus.GYLDIG)
        return Status.GYLDIG
    return Status.MANGELFULL
}

fun mapArbeidsgiverperioder(inntektsmelding: Inntektsmelding): List<Periode> {
    return inntektsmelding.arbeidsgiverperioder.map { p -> Periode(p.fom, p.tom) }
}

fun mapArbeidsgivertype(inntektsmelding: Inntektsmelding): Arbeidsgivertype {
    if (inntektsmelding.arbeidsgiverOrgnummer.isNullOrEmpty()) {
        return Arbeidsgivertype.PRIVAT
    }
    return Arbeidsgivertype.VIRKSOMHET
}

fun mapGjenopptakelseAvNaturalytelser(inntektsmelding: Inntektsmelding): List<GjenopptakelseNaturalytelse> {
    return inntektsmelding.gjenopptakelserNaturalYtelse.map { gjenopptakelse -> GjenopptakelseNaturalytelse(mapNaturalytelseType(gjenopptakelse.naturalytelse), gjenopptakelse.fom, gjenopptakelse.beloepPrMnd) }
}

fun mapOpphørAvNaturalytelser(inntektsmelding: Inntektsmelding): List<OpphoerAvNaturalytelse> {
    return inntektsmelding.opphørAvNaturalYtelse.map { opphør -> OpphoerAvNaturalytelse(mapNaturalytelseType(opphør.naturalytelse), opphør.fom, opphør.beloepPrMnd) }
}

fun mapEndringIRefusjon(inntektsmelding: Inntektsmelding): List<EndringIRefusjon> {
    return inntektsmelding.endringerIRefusjon.map { endring -> EndringIRefusjon(endring.endringsdato, endring.beloep) }
}

fun mapRefusjon(inntektsmelding: Inntektsmelding): Refusjon {
    return Refusjon(inntektsmelding.refusjon.beloepPrMnd, inntektsmelding.refusjon.opphoersdato)
}

fun mapNaturalytelseType(naturalytelseType: no.nav.syfo.domain.inntektsmelding.Naturalytelse?): Naturalytelse {
    return naturalytelseType?.let { naturalytelse ->
        if (Naturalytelse.values().map { it.name }.contains(naturalytelse.name)) Naturalytelse.valueOf(naturalytelse.name) else Naturalytelse.ANNET
    }
        ?: Naturalytelse.ANNET
}
fun mapAvsenderSystem(avsenderSystem: no.nav.syfo.domain.inntektsmelding.AvsenderSystem): AvsenderSystem {
    return AvsenderSystem(
        avsenderSystem.navn,
        avsenderSystem.versjon
    )
}
