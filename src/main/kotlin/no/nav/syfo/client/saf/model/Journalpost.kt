package no.nav.syfo.client.saf.model

import no.nav.syfo.domain.JournalStatus
import java.time.LocalDateTime

data class Journalpost(
    val journalstatus: JournalStatus,
    val datoOpprettet: LocalDateTime,
    val dokumenter: List<Dokument>,
    val avsenderMottaker: SafAvsenderMottaker? = null,
)

data class SafAvsenderMottaker(
    val id: String,
    val type: String,
    val navn: String
)

data class Dokument(
    val dokumentInfoId: String
)
