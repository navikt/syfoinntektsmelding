//package no.nav.syfo.saf.service
//
//import no.nav.syfo.domain.InngaendeJournalpost
//import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
//import java.lang.RuntimeException
//import no.nav.syfo.saf.SafJournalpostClient
//import org.slf4j.LoggerFactory
//
//class SafJournalpostService(
//    val safJournalpostClient: SafJournalpostClient,
//    val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
//    val metrikk: Metrikk,
//) {
//    companion object {
//        private val log = LoggerFactory.getLogger(SafJournalpostService::class.java)
//    }
//
//    suspend fun hentDokumentId(journalpostId: String, token: String): Boolean {
//        val graphQLResponse = safJournalpostClient.getJournalpostMetadata(journalpostId, token)
//
//        if (graphQLResponse == null) {
//            log.error("Kall til SAF feilet for $journalpostId")
//            throw RuntimeException("Klarte ikke hente data fra SAF")
//        }
//        if (graphQLResponse.errors != null) {
//            graphQLResponse.errors.forEach {
//                log.error("Saf kastet error: {} ", it)
//            }
//        }
//
//        if (graphQLResponse.data.journalpost.journalstatus == null) {
//            log.error("Klarte ikke hente data fra SAF {}", journalpostId)
//            throw RuntimeException("Klarte ikke hente data fra SAF")
//        }
//
//        return erJournalfoert(graphQLResponse.data.journalpost.journalstatus)
//    }
//
//    private fun erJournalfoert(journalstatus: String?): Boolean {
//        return journalstatus?.let {
//            it.equals("JOURNALFOERT", true) || it.equals("FERDIGSTILT", true)
//        } ?: false
//    }
//
//
//    private fun hentInntektsmelding(journalpostId: String, arkivReferanse: String): Inntektsmelding {
//        val inngaaendeJournal = inngaaendeJournalConsumer.hentDokumentId(journalpostId)
//        return journalConsumer.hentInntektsmelding(journalpostId, inngaaendeJournal, arkivReferanse)
//    }
//
//    fun ferdigstillJournalpost(saksId: String, inntektsmelding: Inntektsmelding) {
//        val journalpost = hentInngaendeJournalpost(saksId, inntektsmelding)
//        behandleInngaaendeJournalConsumer.oppdaterJournalpost(journalpost)
//        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost)
//        metrikk.tellInntektsmeldingerJournalfort()
//    }
//
//    private fun hentInngaendeJournalpost(gsakId: String, inntektsmelding: Inntektsmelding): InngaendeJournalpost {
//
//        val inngaaendeJournal = inngaaendeJournalConsumer.hentDokumentId(inntektsmelding.journalpostId)
//        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(inntektsmelding.fnr, inntektsmelding.id)
//
//        return InngaendeJournalpost(
//            fnr = inntektsmelding.fnr,
//            gsakId = gsakId,
//            journalpostId = inntektsmelding.journalpostId,
//            dokumentId = inngaaendeJournal.dokumentId,
//            behandlendeEnhetId = behandlendeEnhet,
//            arbeidsgiverOrgnummer = inntektsmelding.arbeidsgiverOrgnummer,
//            arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivatFnr
//        )
//    }
//}
