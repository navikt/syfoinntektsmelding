package no.nav.syfo.consumer.ws

import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.consumer.rest.dokarkiv.DokArkivClient
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.util.MDCOperations
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.FerdigstillJournalfoeringRequest

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

        val behandler = DokArkivClient.Behandler("", "", "")
        runBlocking {
            dokArkivClient.oppdaterJournalpost( journalpostId, inngaendeJournalpost.fnr, behandler, MDCOperations.generateCallId())
        }
    }

    /**
     * Ferdigstiller en journalpost og setter behandlende enhet til 9999
     *
     */
    fun ferdigstillJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        val journalpostId = inngaendeJournalpost.journalpostId

        val request = FerdigstillJournalfoeringRequest()
        request.enhetId = "9999"
        request.journalpostId = journalpostId

        runBlocking {
            dokArkivClient.ferdigstillJournalpost( journalpostId, MDCOperations.generateCallId() )
        }

    }
}
