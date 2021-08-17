package no.nav.syfo.saf.model

import no.nav.syfo.domain.JournalStatus
import java.time.LocalDateTime

data class JournalpostResponse(
    val journalpost: Journalpost
)

data class Journalpost(
    val journalstatus: JournalStatus?,
    val dokumentId : String?,
    val mottattDato: LocalDateTime?
)
