package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.InngaaendeJournal
import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.saf.SafJournalpostClient
import java.time.LocalDateTime

class InngaaendeJournalConsumer(
    private val safJournalpostClient: SafJournalpostClient,
    ) {

    var log = log()

    fun hentDokumentId(journalpostId: String): InngaaendeJournal {
        return runBlocking {
            val response = safJournalpostClient.getJournalpostMetadata(journalpostId)
            InngaaendeJournal(
                dokumentId = response?.data?.journalpost?.dokumenter!![0].dokumentInfoId,
                status = response.data.journalpost.journalstatus,
                mottattDato = response.data.journalpost.datoOpprettet
            )
        }
    }
}

data class InngaaendeJournal (
    val dokumentId: String,
    val status: JournalStatus,
    val mottattDato: LocalDateTime,
)
