package no.nav.syfo.client.saf.model

import java.time.LocalDateTime
import no.nav.syfo.domain.JournalStatus

data class Journalpost(
    val journalstatus: JournalStatus,
    val datoOpprettet: LocalDateTime,
    val dokumenter: List<Dokument>
)

data class Dokument(
    val dokumentInfoId: String
)
