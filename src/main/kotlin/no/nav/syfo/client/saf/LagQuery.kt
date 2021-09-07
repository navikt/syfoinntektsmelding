package no.nav.syfo.client.saf

fun lagQuery(journalpostId: String): String {
    return """
        journalpost(journalpostId: "$journalpostId") {
            tittel,
            journalstatus,
            datoOpprettet,
            opprettetAvNavn,
            dokumenter {
                dokumentInfoId
            }
        }
""".trim()
}
