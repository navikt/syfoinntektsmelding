package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.util.MDCOperations

class BehandleInngaaendeJournalConsumer(private val dokArkivClient: DokArkivClient) {

    var log = log()

    /**
     * Oppdaterer journalposten
     */
    fun oppdaterJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        val journalpostId = inngaendeJournalpost.journalpostId
        val avsenderNr = inngaendeJournalpost.arbeidsgiverOrgnummer
            ?: inngaendeJournalpost.arbeidsgiverPrivat
            ?: throw RuntimeException("Mangler avsender")

        val isFnr = avsenderNr != inngaendeJournalpost.arbeidsgiverOrgnummer
        runBlocking {
            dokArkivClient.oppdaterJournalpost( journalpostId, avsenderNr, isFnr, inngaendeJournalpost.gsakId, MDCOperations.generateCallId())
        }
    }

    /**
     * Ferdigstiller en journalpost og setter behandlende enhet til 9999
     *
     */
    fun ferdigstillJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        val journalpostId = inngaendeJournalpost.journalpostId

        runBlocking {
            dokArkivClient.ferdigstillJournalpost( journalpostId, MDCOperations.generateCallId() )
        }

    }
}