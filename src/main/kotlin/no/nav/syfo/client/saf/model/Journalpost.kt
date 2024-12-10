package no.nav.syfo.client.saf.model

import no.nav.syfo.domain.JournalStatus
import java.time.LocalDateTime

data class Journalpost(
    val journalstatus: JournalStatus,
    val datoOpprettet: LocalDateTime,
    val dokumenter: List<Dokument>,
)

data class Dokument(
    val dokumentInfoId: String,
)
