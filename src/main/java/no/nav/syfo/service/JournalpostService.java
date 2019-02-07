package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer;
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.JournalConsumer;
import no.nav.syfo.domain.InngaaendeJournal;
import no.nav.syfo.domain.InngaendeJournalpost;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.util.Metrikk;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JournalpostService {

    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    private BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer;
    private JournalConsumer journalConsumer;
    private BehandlendeEnhetConsumer behandlendeEnhetConsumer;
    private Metrikk metrikk;

    public JournalpostService(
            InngaaendeJournalConsumer inngaaendeJournalConsumer,
            BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer,
            JournalConsumer journalConsumer,
            BehandlendeEnhetConsumer behandlendeEnhetConsumer,
            Metrikk metrikk) {
        this.inngaaendeJournalConsumer = inngaaendeJournalConsumer;
        this.behandleInngaaendeJournalConsumer = behandleInngaaendeJournalConsumer;
        this.journalConsumer = journalConsumer;
        this.behandlendeEnhetConsumer = behandlendeEnhetConsumer;
        this.metrikk = metrikk;
    }

    public Inntektsmelding hentInntektsmelding(String journalpostId) {
        InngaaendeJournal inngaaendeJournal = inngaaendeJournalConsumer.hentDokumentId(journalpostId);
        return journalConsumer.hentInntektsmelding(journalpostId, inngaaendeJournal);
    }

    public void ferdigstillJournalpost(String saksId, Inntektsmelding inntektsmelding) {
        InngaendeJournalpost journalpost = hentInngaendeJournalpost(saksId, inntektsmelding);
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(journalpost);
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost);
        metrikk.tellInntektsmeldingerJournalfort();
    }

    private InngaendeJournalpost hentInngaendeJournalpost(String gsakId, Inntektsmelding inntektsmelding) {
        InngaaendeJournal inngaaendeJournal = inngaaendeJournalConsumer.hentDokumentId(inntektsmelding.getJournalpostId());
        String behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(inntektsmelding.getFnr());

        return InngaendeJournalpost.builder()
                .fnr(inntektsmelding.getFnr())
                .gsakId(gsakId)
                .journalpostId(inntektsmelding.getJournalpostId())
                .dokumentId(inngaaendeJournal.getDokumentId())
                .behandlendeEnhetId(behandlendeEnhet)
                .arbeidsgiverOrgnummer(inntektsmelding.getArbeidsgiverOrgnummer())
                .orgnummerPrivatperson(inntektsmelding.getOrgnummerPrivatperson())
                .build();
    }
}
