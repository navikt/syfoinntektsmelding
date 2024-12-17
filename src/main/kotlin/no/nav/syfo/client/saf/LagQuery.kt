package no.nav.syfo.client.saf

fun lagQuery(journalpostId: String): String =
    """query {
        journalpost(journalpostId: "$journalpostId") {
            tittel,
            journalstatus,
            datoOpprettet,
            opprettetAvNavn,
            dokumenter {
                dokumentInfoId
            }
        }
}""".trim()
