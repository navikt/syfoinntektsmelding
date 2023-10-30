package no.nav.syfo.domain

import no.nav.syfo.client.saf.model.SafAvsenderMottaker
import java.time.LocalDateTime

data class InngaaendeJournal(
    val dokumentId: String,
    val status: JournalStatus,
    val mottattDato: LocalDateTime,
    val avsenderMottaker: SafAvsenderMottaker? = null,
)

/**
 * https://confluence.adeo.no/display/BOA/saf+-+Enums
 */
enum class JournalStatus {
    MOTTATT, // Tidligere: MIDLERTIDIG
    UKJENT, // Tidligere: ANNET
    FERDIGSTILT, // Tidligere: ENDELIG
    MIDLERTIDIG,
    UTGAAR,
    JOURNALFOERT,
    EKSPEDERT,
    UNDER_ARBEID,
    FEILREGISTRERT,
    AVBRUTT,
    UKJENT_BRUKER,
    RESERVERT,
    OPPLASTING_DOKUMENT,
}
