package no.nav.syfo.domain.inntektsmelding

import no.nav.syfo.domain.Periode
import java.time.LocalDate

/**
 * Basert på Inntekt fra Simba
 * @see no.nav.helsearbeidsgiver.domene.inntektsmelding.deprecated.Inntekt
 **/

data class RapportertInntekt(
    val bekreftet: Boolean,
    val beregnetInntekt: Double,
    val endringAarsak: String? = null,
    val endringAarsakData: SpinnInntektEndringAarsak? = null,
    val manueltKorrigert: Boolean,
)
data class SpinnInntektEndringAarsak(
    val aarsak: String,
    val perioder: List<Periode>? = null,
    val gjelderFra: LocalDate? = null,
    val bleKjent: LocalDate? = null
)
