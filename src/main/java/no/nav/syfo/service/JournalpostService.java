package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.InngaendeJournalpost;
import no.nav.syfo.domain.Inntektsmelding;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JournalpostService {

    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    private BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer;
    private JournalConsumer journalConsumer;
    private BehandlendeEnhetConsumer behandlendeEnhetConsumer;

    public JournalpostService(InngaaendeJournalConsumer inngaaendeJournalConsumer, BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer, JournalConsumer journalConsumer, BehandlendeEnhetConsumer behandlendeEnhetConsumer) {
        this.inngaaendeJournalConsumer = inngaaendeJournalConsumer;
        this.behandleInngaaendeJournalConsumer = behandleInngaaendeJournalConsumer;
        this.journalConsumer = journalConsumer;
        this.behandlendeEnhetConsumer = behandlendeEnhetConsumer;
    }

    public Inntektsmelding hentInntektsmelding(String journalpostId) {
        String dokumentId = inngaaendeJournalConsumer.hentDokumentId(journalpostId);
        return journalConsumer.hentInntektsmelding(journalpostId, dokumentId);
    }

    public void ferdigstillJournalpost(String saksId, Inntektsmelding inntektsmelding) {
        InngaendeJournalpost journalpost = hentInngaendeJournalpost(saksId, inntektsmelding);
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(journalpost);
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost);
    }

    private InngaendeJournalpost hentInngaendeJournalpost(String gsakId, Inntektsmelding inntektsmelding) {
        String dokumentId = inngaaendeJournalConsumer.hentDokumentId(inntektsmelding.getJournalpostId());
        String behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(inntektsmelding.getFnr());

        log.info("Hentet journalpost: ", inntektsmelding.getJournalpostId());

        return InngaendeJournalpost.builder()
                .fnr(inntektsmelding.getFnr())
                .gsakId(gsakId)
                .journalpostId(inntektsmelding.getJournalpostId())
                .dokumentId(dokumentId)
                .behandlendeEnhetId(behandlendeEnhet)
                .arbeidsgiverOrgnummer(inntektsmelding.getArbeidsgiverOrgnummer())
                .build();
    }
}
