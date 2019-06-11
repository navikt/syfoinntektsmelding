package no.nav.syfo.domain


data class Inntektsmelding(
    val fnr: String,
    val arbeidsgiverOrgnummer: String? = null,
    val arbeidsgiverPrivat: String? = null,
    val journalpostId: String,
    val arbeidsforholdId: String? = null,
    val arsakTilInnsending: String,
    val status: String,
    val arbeidsgiverperioder: List<Periode> = emptyList()
)

