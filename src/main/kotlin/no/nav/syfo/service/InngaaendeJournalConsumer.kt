package no.nav.syfo.service

import log
import no.nav.syfo.domain.InngaaendeJournal
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.saf.SafJournalpostClient

class InngaaendeJournalConsumer(
    private val safJournalpostClient: SafJournalpostClient,
    ) {

    var log = log()

    fun hentDokumentId(journalpostId: String): InngaaendeJournal {
        return runBlocking {
            val journalpost = safJournalpostClient.getJournalpostMetadata(journalpostId)
            InngaaendeJournal(
                dokumentId = journalpost?.dokumenter!![0].dokumentInfoId,
                status = journalpost.journalstatus,
                mottattDato = journalpost.datoOpprettet
            )
        }
    }
}
