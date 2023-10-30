package no.nav.syfo.service

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.client.dokarkiv.AvsenderMottaker
import no.nav.syfo.client.dokarkiv.DokArkivClient
import no.nav.syfo.client.dokarkiv.mapFeilregistrertRequest
import no.nav.syfo.client.dokarkiv.mapOppdaterRequest
import no.nav.syfo.client.saf.model.SafAvsenderMottaker
import no.nav.syfo.domain.InngaendeJournalpost

class BehandleInngaaendeJournalConsumer(private val dokArkivClient: DokArkivClient) {

    /**
     * Oppdaterer journalposten
     */

    private val sikkerlogger = sikkerLogger()
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
        inngaendeJournalpost.safAvsenderMottaker?.erLikAvsenderMottaker(req.avsenderMottaker!!)?.also {
            if (!it) {
                sikkerlogger.info("Avsender/mottaker er endret for journalpost $journalpostId, fra saf: ${inngaendeJournalpost.safAvsenderMottaker}, til dokarkiv: ${req.avsenderMottaker}")
            } else sikkerlogger.info("Avsender/mottaker er ikke endret for journalpost $journalpostId")
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

fun SafAvsenderMottaker.erLikAvsenderMottaker(other: AvsenderMottaker): Boolean {
    return this.id == other.id && this.type == other.idType && this.navn == other.navn
}
