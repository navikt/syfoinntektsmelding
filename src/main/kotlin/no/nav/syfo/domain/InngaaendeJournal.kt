package no.nav.syfo.domain

data class InngaaendeJournal(
    val dokumentId: String,
    val status: JournalStatus
)

enum class JournalStatus {
    MIDLERTIDIG, ANNET
}
