package no.nav.syfo.client.saf

fun lagQuery(journalpostId: String) : String {
    return """
        journalpost(journalpostId: $journalpostId) {
            journalstatus,
            datoOpprettet,
            dokumenter {
              dokumentInfoId
            }
        }"""
}
