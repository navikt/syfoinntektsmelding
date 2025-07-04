package no.nav.syfo.client.saf.model

data class JournalResponse(
    val data: SafJournalData? = null,
    val errors: List<ResponseError>? = null,
    val timestamp: String? = null,
    val status: Number? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
)
