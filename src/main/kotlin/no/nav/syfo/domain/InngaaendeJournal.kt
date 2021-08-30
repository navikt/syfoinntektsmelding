package no.nav.syfo.domain

import java.time.LocalDateTime

data class InngaaendeJournal(
        val dokumentId: String,
        val status: JournalStatus,
        val mottattDato: LocalDateTime
)

enum class JournalStatus {
    MIDLERTIDIG, ANNET, ENDELIG, UTGAAR
}
