package no.nav.syfo.consumer.ws

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
            val response = safJournalpostClient.getJournalpostMetadata(journalpostId)
            InngaaendeJournal(
                dokumentId = response.journalpost?.dokumenter!![0].dokumentInfoId,
                status = response.journalpost.journalstatus,
                mottattDato = response.journalpost.datoOpprettet
            )
        }
    }
}
