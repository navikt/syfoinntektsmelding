package no.nav.syfo.consumer.ws

import no.nav.syfo.domain.Periode


data class XMLInntektsmelding (
    val arbeidsforholdId: String? = null,
    val perioder: List<Periode>,
    val arbeidstakerFnr: String,
    val virksomhetsnummer: String? = null,
    val arbeidsgiverPrivat:String? = null,
    val aarsakTilInnsending: String
)
