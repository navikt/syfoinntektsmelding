package no.nav.syfo.client.saf

import no.nav.syfo.client.saf.model.Journalpost

data class SafJournalResponse (
    val journalpost: Journalpost,
    val errors: List<ResponseError>?
)

data class ResponseError(
    val message: String?,
    val locations: List<ErrorLocation>?,
    val path: List<String>?,
    val extensions: ErrorExtension?
)

data class ErrorLocation(
    val line: String?,
    val column: String?
)

data class ErrorExtension(
    val code: String?,
    val classification: String?
)
