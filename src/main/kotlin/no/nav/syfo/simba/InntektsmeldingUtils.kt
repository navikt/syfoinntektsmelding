package no.nav.syfo.simba

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding

fun Inntektsmelding.Type.sendTilSpleis(): Boolean =
    when (this) {
        is Inntektsmelding.Type.Fisker, is Inntektsmelding.Type.UtenArbeidsforhold, is Inntektsmelding.Type.Behandlingsdager -> false
        is Inntektsmelding.Type.Selvbestemt, is Inntektsmelding.Type.Forespurt, is Inntektsmelding.Type.ForespurtEkstern -> true
    }
