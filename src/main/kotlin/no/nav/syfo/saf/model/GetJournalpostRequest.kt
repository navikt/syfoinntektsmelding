package no.nav.syfo.saf.model

data class GetJournalpostRequest(
    val query: String,
    val variables: GetJournalpostVariables
)
