package no.nav.syfo.client.saf

fun lagQuery(journalpostId: String): String {
    return """query {
        journalpost(journalpostId: "$journalpostId") {
            tittel,
            journalstatus,
            datoOpprettet,
            opprettetAvNavn,
            dokumenter {
                dokumentInfoId
            },
            avsenderMottaker {
                id
                type
                navn
            }
        }
}""".trim()
}
