package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.ArbeidsfordelingConsumer;
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.JournalConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.domain.InngaendeJournalpost;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JournalpostService {

    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    private JournalConsumer journalConsumer;
    private PersonConsumer personConsumer;
    private ArbeidsfordelingConsumer arbeidsfordelingConsumer;

    public JournalpostService(InngaaendeJournalConsumer inngaaendeJournalConsumer, JournalConsumer journalConsumer, ArbeidsfordelingConsumer arbeidsfordelingConsumer, PersonConsumer personConsumer) {
        this.inngaaendeJournalConsumer = inngaaendeJournalConsumer;
        this.journalConsumer = journalConsumer;
        this.personConsumer = personConsumer;
        this.arbeidsfordelingConsumer = arbeidsfordelingConsumer;
    }

    public InngaendeJournalpost hentInngaendeJournalpost(String journalpostId, String gsakId, String orgnummer) {
        String dokumentId = inngaaendeJournalConsumer.hentDokumentId(journalpostId);
        String fnr = journalConsumer.hentInntektsmelding(journalpostId, dokumentId).getFnr();
        String geografiskTilknytning = personConsumer.hentGeografiskTilknytning(fnr);
        String behandlendeEnhetId = arbeidsfordelingConsumer.finnBehandlendeEnhet(geografiskTilknytning);

        log.info("Hentet journalpost: ", journalpostId);

        return InngaendeJournalpost.builder()
                .fnr(fnr)
                .gsakId(gsakId)
                .journalpostId(journalpostId)
                .dokumentId(dokumentId)
                .behandlendeEnhetId(behandlendeEnhetId)
                .arbeidsgiverOrgnummer(orgnummer)
                .build();
    }
}
