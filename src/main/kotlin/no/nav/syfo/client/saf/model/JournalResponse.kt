package no.nav.syfo.client.saf.model

import no.nav.syfo.client.saf.ResponseError
import no.nav.syfo.client.saf.SafJournalData

data class JournalResponse(
    val data: SafJournalData? = null,
    val errors: List<ResponseError>? = null,
    val timestamp: String? = null,
    val status: Number? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
)
