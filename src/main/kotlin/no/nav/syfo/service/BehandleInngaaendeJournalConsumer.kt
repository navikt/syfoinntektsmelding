package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.dokarkiv.OppdaterJournalpostRequest
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.util.MDCOperations
import java.util.UUID

class BehandleInngaaendeJournalConsumer(private val dokArkivClient: DokArkivClient) {

    var log = log()

    /**
     * Oppdaterer journalposten
     */
    fun oppdaterJournalpost(fnr: String, inngaendeJournalpost: InngaendeJournalpost) {
        val journalpostId = inngaendeJournalpost.journalpostId
        val avsenderNr = inngaendeJournalpost.arbeidsgiverOrgnummer
            ?: inngaendeJournalpost.arbeidsgiverPrivat
            ?: throw RuntimeException("Mangler avsender")
        val isArbeidsgiverFnr = avsenderNr != inngaendeJournalpost.arbeidsgiverOrgnummer
        runBlocking {
            dokArkivClient.oppdaterJournalpost(
                journalpostId,
                fnr,
                avsenderNr,
                inngaendeJournalpost.arbeidsgiverNavn,
                isArbeidsgiverFnr,
                MDCOperations.generateCallId()
            )
        }
    }

    /**
     * Ferdigstiller en journalpost og setter behandlende enhet til 9999
     *
     */
    fun ferdigstillJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        val journalpostId = inngaendeJournalpost.journalpostId
        runBlocking {
            dokArkivClient.ferdigstillJournalpost(journalpostId, MDCOperations.generateCallId())
        }
    }

    fun feilregistrerJournalpost(journalpostId: String) {
        val request = OppdaterJournalpostRequest(tittel="Duplikat inntektsmelding")
        val callId = UUID.randomUUID().toString()
        runBlocking {
            dokArkivClient.oppdaterJournalpost(journalpostId, request, callId)
        }
        runBlocking {
            dokArkivClient.feilregistrerJournalpost(journalpostId, callId)
        }
    }
}
