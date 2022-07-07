package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.dokarkiv.mapFeilregistrertRequest
import no.nav.syfo.client.dokarkiv.mapOppdaterRequest
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.util.MDCOperations

class BehandleInngaaendeJournalConsumer(private val dokArkivClient: DokArkivClient) {

    /**
     * Oppdaterer journalposten
     */
    fun oppdaterJournalpost(fnr: String, inngaendeJournalpost: InngaendeJournalpost, feilregistrert: Boolean) {
        val journalpostId = inngaendeJournalpost.journalpostId
        val avsenderNr = inngaendeJournalpost.arbeidsgiverOrgnummer
            ?: inngaendeJournalpost.arbeidsgiverPrivat
            ?: throw RuntimeException("Mangler avsender")
        val isArbeidsgiverFnr = avsenderNr != inngaendeJournalpost.arbeidsgiverOrgnummer
        val req = if (feilregistrert) {
            mapFeilregistrertRequest(fnr, avsenderNr, inngaendeJournalpost.arbeidsgiverNavn, isArbeidsgiverFnr, inngaendeJournalpost.dokumentId)
        } else {
            mapOppdaterRequest(fnr, avsenderNr, inngaendeJournalpost.arbeidsgiverNavn, isArbeidsgiverFnr)
        }
        runBlocking {
            dokArkivClient.oppdaterJournalpost(
                journalpostId,
                req,
                MDCOperations.generateCallId()
            )
        }
    }

    /**
     * Ferdigstiller en journalpost og setter behandlende enhet til 9999
     *
     */
    fun ferdigstillJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        runBlocking {
            dokArkivClient.ferdigstillJournalpost(inngaendeJournalpost.journalpostId, MDCOperations.generateCallId())
        }
    }

    fun feilregistrerJournalpost(journalpostId: String) {
        runBlocking {
            dokArkivClient.feilregistrerJournalpost(journalpostId, MDCOperations.generateCallId())
        }
    }
}
