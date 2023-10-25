package no.nav.syfo.domain.inntektsmelding

/**
 * Basert på Inntekt fra Simba
 * @see no.nav.helsearbeidsgiver.domene.inntektsmelding.Inntekt

 **/

data class RapportertInntekt(
    val bekreftet: Boolean,
    val beregnetInntekt: Double,
    val endringAarsak: String? = null,
    val manueltKorrigert: Boolean,
)
