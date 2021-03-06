package no.nav.syfo.service

import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.JournalConsumer
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.util.Metrikk

class JournalpostService(
        private val inngaaendeJournalConsumer: InngaaendeJournalConsumer,
        private val behandleInngaaendeJournalConsumer: BehandleInngaaendeJournalConsumer,
        private val journalConsumer: JournalConsumer,
        private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
        private val metrikk: Metrikk
) {

    fun hentInntektsmelding(journalpostId: String, arkivReferanse: String): Inntektsmelding {
        val inngaaendeJournal = inngaaendeJournalConsumer.hentDokumentId(journalpostId)
        return journalConsumer.hentInntektsmelding(journalpostId, inngaaendeJournal, arkivReferanse)
    }

    fun ferdigstillJournalpost(saksId: String, inntektsmelding: Inntektsmelding) {
        val journalpost = hentInngaendeJournalpost(saksId, inntektsmelding)
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(journalpost)
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost)
        metrikk.tellInntektsmeldingerJournalfort()
    }

    private fun hentInngaendeJournalpost(gsakId: String, inntektsmelding: Inntektsmelding): InngaendeJournalpost {
        val inngaaendeJournal = inngaaendeJournalConsumer.hentDokumentId(inntektsmelding.journalpostId)
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(inntektsmelding.fnr, inntektsmelding.id)

        return InngaendeJournalpost(
                fnr = inntektsmelding.fnr,
                gsakId = gsakId,
                journalpostId = inntektsmelding.journalpostId,
                dokumentId = inngaaendeJournal.dokumentId,
                behandlendeEnhetId = behandlendeEnhet,
                arbeidsgiverOrgnummer = inntektsmelding.arbeidsgiverOrgnummer,
                arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivatFnr
        )
    }
}
