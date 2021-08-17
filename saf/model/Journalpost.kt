package no.nav.syfo.saf.model

data class JournalpostResponse(
    val journalpost: Journalpost
)

data class Journalpost(
    val journalstatusa: String?,
    val dokumentId : String?
)
