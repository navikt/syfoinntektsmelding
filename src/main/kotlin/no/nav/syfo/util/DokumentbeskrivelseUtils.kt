package no.nav.syfo.util

import no.nav.helsearbeidsgiver.utils.pipe.orDefault
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.tilKortFormat

fun Inntektsmelding.tilDokumentbeskrivelse(): String {
    val orgnr = this.arbeidsgiverOrgnummer.let { if (it.isNullOrBlank()) "(ingen orgnr)" else it }
    val agp = this.arbeidsgiverperioder.tilKortFormat().orDefault("(ingen agp)")
    return "Inntektsmelding-$orgnr-$agp"
}
