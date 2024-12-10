package no.nav.syfo.client.dokarkiv

/**
 * val journalpostId = inngaendeJournalpost.journalpostId
 * val avsenderNr = inngaendeJournalpost.arbeidsgiverOrgnummer
 * ?: inngaendeJournalpost.arbeidsgiverPrivat
 * ?: throw RuntimeException("Mangler avsender")
 *
 * journalpostId = inngaendeJournalpost.journalpostId
 * avsender (id=avsenderNr og navn="Arbeidsgiver")
 * arkivSak (arkivSakId = inngaendeJournalpost.gsakId og arkivSakSystem = "FS22")
 *
 **/
data class OppdaterJournalpostRequest(
    val bruker: Bruker? = null,
    val avsenderMottaker: AvsenderMottaker? = null,
    val sak: Sak? = null,
    val tema: String? = null,
    val tittel: String? = null,
    val dokumenter: List<Dokument>? = null,
)

data class Dokument(
    val dokumentInfoId: String,
    val tittel: String? = null,
)

data class Bruker(
    val id: String,
    val idType: String,
)

data class AvsenderMottaker(
    val id: String,
    val idType: String,
    val navn: String,
)

data class Sak(
    val sakstype: String,
    val arkivsaksystem: String? = null,
)

fun mapOppdaterRequest(fnr: String): OppdaterJournalpostRequest {
    return OppdaterJournalpostRequest(
        bruker =
            Bruker(
                fnr,
                "FNR",
            ),
        sak = Sak("GENERELL_SAK"),
        tema = "SYK",
    )
}
