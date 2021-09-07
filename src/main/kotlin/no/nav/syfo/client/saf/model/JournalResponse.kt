package no.nav.syfo.client.saf.model

import no.nav.syfo.client.saf.ResponseError
import no.nav.syfo.client.saf.SafJournalData
import java.time.LocalDateTime

data class JournalResponse (
    val data: SafJournalData? = null,
    val errors: List<ResponseError>? = null,
    val timestamp: LocalDateTime? = null,
    val status: Number? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
)
