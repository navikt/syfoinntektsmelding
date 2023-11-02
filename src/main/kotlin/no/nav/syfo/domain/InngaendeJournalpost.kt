package no.nav.syfo.domain

data class InngaendeJournalpost(
    val fnr: String,
    val journalpostId: String,
    val dokumentId: String,
    val behandlendeEnhetId: String,
)
