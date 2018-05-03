package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.ArbeidsfordelingConsumer;
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.domain.InngaendeJournalpost;
import no.nav.syfo.domain.Inntektsmelding;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JournalpostService {

    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    private PersonConsumer personConsumer;
    private ArbeidsfordelingConsumer arbeidsfordelingConsumer;

    public JournalpostService(InngaaendeJournalConsumer inngaaendeJournalConsumer, ArbeidsfordelingConsumer arbeidsfordelingConsumer, PersonConsumer personConsumer) {
        this.inngaaendeJournalConsumer = inngaaendeJournalConsumer;
        this.personConsumer = personConsumer;
        this.arbeidsfordelingConsumer = arbeidsfordelingConsumer;
    }

    public InngaendeJournalpost hentInngaendeJournalpost(String journalpostId, String gsakId, Inntektsmelding inntektsmelding) {
        String dokumentId = inngaaendeJournalConsumer.hentDokumentId(journalpostId);
        String geografiskTilknytning = personConsumer.hentGeografiskTilknytning(inntektsmelding.getFnr());
        String behandlendeEnhetId = arbeidsfordelingConsumer.finnBehandlendeEnhet(geografiskTilknytning);

        log.info("Hentet journalpost: ", journalpostId);

        return InngaendeJournalpost.builder()
                .fnr(inntektsmelding.getFnr())
                .gsakId(gsakId)
                .journalpostId(journalpostId)
                .dokumentId(dokumentId)
                .behandlendeEnhetId(behandlendeEnhetId)
                .arbeidsgiverOrgnummer(inntektsmelding.getArbeidsgiverOrgnummer())
                .build();
    }
}
