package no.nav.syfo.domain

import java.time.LocalDateTime
import no.nav.helse.arbeidsgiver.integrasjoner.dokarkiv.graphql.*

data class InngaaendeJournal(
    val dokumentId: String,
    val status: Journalstatus,
    val mottattDato: LocalDateTime?
)
