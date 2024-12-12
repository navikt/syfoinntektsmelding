package no.nav.syfo.service

import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.util.Metrikk

class JournalpostService(
    private val inngaaendeJournalConsumer: InngaaendeJournalConsumer,
    private val behandleInngaaendeJournalConsumer: BehandleInngaaendeJournalConsumer,
    private val journalConsumer: JournalConsumer,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val metrikk: Metrikk,
) {
    fun hentInntektsmelding(
        journalpostId: String,
        arkivReferanse: String,
    ): Inntektsmelding {
        return journalConsumer.hentInntektsmelding(journalpostId, arkivReferanse)
    }

    fun ferdigstillJournalpost(inntektsmelding: Inntektsmelding) {
        val journalpost = hentInngaendeJournalpost(inntektsmelding)
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(inntektsmelding.fnr, journalpost, false)
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost)
        metrikk.tellInntektsmeldingerJournalfort()
    }

    private fun hentInngaendeJournalpost(inntektsmelding: Inntektsmelding): InngaendeJournalpost {
        val inngaaendeJournal = inngaaendeJournalConsumer.hentDokumentId(inntektsmelding.journalpostId)
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(inntektsmelding.fnr, inntektsmelding.id)

        return InngaendeJournalpost(
            fnr = inntektsmelding.fnr,
            journalpostId = inntektsmelding.journalpostId,
            dokumentId = inngaaendeJournal.dokumentId,
            behandlendeEnhetId = behandlendeEnhet,
        )
    }

    fun feilregistrerJournalpost(inntektsmelding: Inntektsmelding) {
        val journalpost = hentInngaendeJournalpost(inntektsmelding)
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(inntektsmelding.fnr, journalpost, true)
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost)
        behandleInngaaendeJournalConsumer.feilregistrerJournalpost(inntektsmelding.journalpostId)
    }
}
