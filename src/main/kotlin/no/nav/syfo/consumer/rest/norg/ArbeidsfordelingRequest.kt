package no.nav.syfo.consumer.rest.norg

data class ArbeidsfordelingRequest(
    var behandlingstema	: String? = null,
    var behandlingstype	: String? = null,
    var diskresjonskode	: String? = null,
    var enhetNummer	: String? = null,
    var geografiskOmraade	: String? = null,
    var oppgavetype	: String? = null,
    var skjermet:	Boolean? = null,
    var tema	: String? = null,
    var temagruppe	: String? = null
)
