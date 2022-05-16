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
    val bruker: Bruker?,
    val avsenderMottaker: AvsenderMottaker?,
    val sak: Sak?
)

data class Bruker(
    val id: String,
    val idType: String
)

data class AvsenderMottaker(
    val id: String,
    val navn: String
)

data class Sak(
    val sakstype: String,
    val arkivsaksystem: String,
)
