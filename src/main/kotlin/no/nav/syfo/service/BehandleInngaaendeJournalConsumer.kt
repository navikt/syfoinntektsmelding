package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.dokarkiv.mapFeilregistrertRequest
import no.nav.syfo.client.dokarkiv.mapOppdaterRequest
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.util.tilDokumentbeskrivelse

class BehandleInngaaendeJournalConsumer(
    private val dokArkivClient: DokArkivClient,
) {
    /**
     * Oppdaterer journalposten
     */

    fun oppdaterJournalpost(
        inntektsmelding: Inntektsmelding,
        inngaendeJournalpost: InngaendeJournalpost,
        feilregistrert: Boolean,
    ) {
        val journalpostId = inngaendeJournalpost.journalpostId
        val req =
            if (feilregistrert) {
                mapFeilregistrertRequest(inntektsmelding.fnr, inngaendeJournalpost.dokumentId)
            } else {
                mapOppdaterRequest(inntektsmelding.fnr, inngaendeJournalpost.dokumentId, inntektsmelding.tilDokumentbeskrivelse())
            }
        runBlocking {
            dokArkivClient.oppdaterJournalpost(
                journalpostId,
                req,
                MdcUtils.getCallId(),
            )
        }
    }

    /**
     * Ferdigstiller en journalpost og setter behandlende enhet til 9999
     *
     */
    fun ferdigstillJournalpost(inngaendeJournalpost: InngaendeJournalpost) {
        runBlocking {
            dokArkivClient.ferdigstillJournalpost(inngaendeJournalpost.journalpostId, MdcUtils.getCallId())
        }
    }

    fun feilregistrerJournalpost(journalpostId: String) {
        runBlocking {
            dokArkivClient.feilregistrerJournalpost(journalpostId, MdcUtils.getCallId())
        }
    }
}
