package no.nav.syfo.client.saf

import no.nav.syfo.client.saf.model.Journalpost

data class SafJournalData(
    val journalpost: Journalpost? = null
)

data class ResponseError(
    val message: String?,
    val locations: List<ErrorLocation>?,
    val path: List<String>?,
    val extensions: ErrorExtension?
)

data class ErrorLocation(
    val line: Number?,
    val column: Number?
)

data class ErrorExtension(
    val code: String?,
    val classification: String?
)
