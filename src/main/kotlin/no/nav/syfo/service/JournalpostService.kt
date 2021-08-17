package no.nav.syfo.service

import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.domain.InngaendeJournalpost
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.saf.SafDokumentClient
import no.nav.syfo.saf.SafJournalpostClient
import no.nav.syfo.util.Metrikk

class JournalpostService(
    private val inngaaendeJournalConsumer: SafJournalpostClient,
    private val behandleInngaaendeJournalConsumer: BehandleInngaaendeJournalConsumer,
    private val journalConsumer: SafDokumentClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val metrikk: Metrikk,
    private val stsClient: AccessTokenProvider
) {

    fun hentInntektsmelding(journalpostId: String, arkivReferanse: String) : Inntektsmelding? {
        runBlocking {
            val inngaaendeJournal = inngaaendeJournalConsumer.getJournalpostMetadata(journalpostId,stsClient.getToken())
            return@runBlocking journalConsumer.hentDokument(journalpostId, inngaaendeJournal?.data?.journalpost?.dokumentId!!, arkivReferanse)
        }
        return null;
    }

    suspend fun ferdigstillJournalpost(saksId: String, inntektsmelding: Inntektsmelding) {
        val journalpost = hentInngaendeJournalpost(saksId, inntektsmelding)
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(journalpost)
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost)
        metrikk.tellInntektsmeldingerJournalfort()
    }

    private suspend fun hentInngaendeJournalpost(gsakId: String, inntektsmelding: Inntektsmelding): InngaendeJournalpost {
        val inngaaendeJournal = hentInntektsmelding(inntektsmelding.journalpostId,gsakId)
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(inntektsmelding.fnr, inntektsmelding.id)

        val safJournalpostClient = SafJournalpostClient(HttpClient(),"","")
        val dokumentId = safJournalpostClient.getJournalpostMetadata(inntektsmelding.journalpostId,stsClient.getToken())?.data?.journalpost?.dokumentId

        return InngaendeJournalpost(
                fnr = inntektsmelding.fnr,
                gsakId = gsakId,
                journalpostId = inntektsmelding.journalpostId,
                dokumentId = dokumentId!!,
                behandlendeEnhetId = behandlendeEnhet,
                arbeidsgiverOrgnummer = inntektsmelding.arbeidsgiverOrgnummer,
                arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivatFnr
        )
    }
}
