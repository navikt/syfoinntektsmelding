package no.nav.syfo.consumer.rest.dokarkiv

import java.time.LocalDate

data class FerdigstillRequest (
    val journalfoerendeEnhet: String,
    val journalfortAvNavn: String? = null,
    val opprettetAvNavn: String? = null,
    val datoJournal: LocalDate? = null,
    val datoSendtPrint: LocalDate? = null,
)
