package no.nav.syfo.service

import io.ktor.client.*
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.rest.inntektsmelding.InntektsmeldingMapper
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

    suspend fun hentInntektsmelding(journalpostId: String, arkivReferanse: String) : Inntektsmelding? {
        val journalpostResponse = inngaaendeJournalConsumer.getJournalpostMetadata(journalpostId, stsClient.getToken())
        val bytes: ByteArray? = journalConsumer.hentDokument(journalpostId, journalpostResponse?.data?.journalpost?.dokumentId!!, stsClient.getToken())
        val mottattDato = journalpostResponse.data.journalpost.mottattDato!!
        val journalStatus = journalpostResponse.data.journalpost.journalstatus!!
        return InntektsmeldingMapper().mapInntektsmelding(bytes!!, journalpostId, mottattDato, journalpostId, journalStatus, arkivReferanse)
    }

    suspend fun ferdigstillJournalpost(saksId: String, inntektsmelding: Inntektsmelding) {
        val journalpost = hentInngaendeJournalpost(saksId, inntektsmelding)
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(journalpost)
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost)
        metrikk.tellInntektsmeldingerJournalfort()
    }

    private suspend fun hentInngaendeJournalpost(gsakId: String, inntektsmelding: Inntektsmelding): InngaendeJournalpost {
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(inntektsmelding.fnr, inntektsmelding.id)
        val safJournalpostClient = SafJournalpostClient(HttpClient(),"",SafJournalpostClient::class.java.getResource("/graphql/getJournalpostDokumentId.graphql").readText().replace(Regex("[\n\t]"), ""))
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
