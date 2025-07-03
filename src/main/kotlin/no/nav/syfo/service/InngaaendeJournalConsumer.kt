package no.nav.syfo.service

import no.nav.syfo.client.saf.SafJournalpostClient
import no.nav.syfo.domain.InngaaendeJournal

class InngaaendeJournalConsumer(
    private val safJournalpostClient: SafJournalpostClient,
) {
    fun hentDokumentId(journalpostId: String): InngaaendeJournal {
        val journalpost = safJournalpostClient.getJournalpostMetadata(journalpostId)
        return InngaaendeJournal(
            dokumentId = journalpost?.dokumenter!![0].dokumentInfoId,
            status = journalpost.journalstatus,
            mottattDato = journalpost.datoOpprettet,
        )
    }
}
