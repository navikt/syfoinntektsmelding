package no.nav.syfo.saf.model

data class JournalpostResponse(
    val journalpost: Journalpost
)

data class Journalpost(
    val journalstatus: String?,
    val dokumentId : String?
)
