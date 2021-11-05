package no.nav.syfo.util

import no.nav.syfo.domain.inntektsmelding.Gyldighetsstatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding

fun validerInntektsmelding(inntektsmelding: Inntektsmelding): Gyldighetsstatus {
    var gyldighet = Gyldighetsstatus.GYLDIG
    if (!erArbeidsforholdGyldig(inntektsmelding))
        gyldighet = Gyldighetsstatus.MANGELFULL
    return gyldighet
}

private fun erArbeidsforholdGyldig(inntektsmelding: Inntektsmelding): Boolean {
    if ((inntektsmelding.arbeidsgiverOrgnummer.isNullOrEmpty() && inntektsmelding.arbeidsgiverPrivatFnr.isNullOrEmpty()))
        return false
    if ((!inntektsmelding.arbeidsgiverOrgnummer.isNullOrEmpty() && !inntektsmelding.arbeidsgiverPrivatFnr.isNullOrEmpty()))
        return false
    return true
}
