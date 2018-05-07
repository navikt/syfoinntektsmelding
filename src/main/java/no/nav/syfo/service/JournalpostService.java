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
    private PersonConsumer personConsumer;
    private ArbeidsfordelingConsumer arbeidsfordelingConsumer;
    private BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer;
    private JournalConsumer journalConsumer;
    private PeriodeService periodeService;

    public JournalpostService(InngaaendeJournalConsumer inngaaendeJournalConsumer, ArbeidsfordelingConsumer arbeidsfordelingConsumer, PersonConsumer personConsumer, BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer, JournalConsumer journalConsumer, PeriodeService periodeService) {
        this.inngaaendeJournalConsumer = inngaaendeJournalConsumer;
        this.personConsumer = personConsumer;
        this.arbeidsfordelingConsumer = arbeidsfordelingConsumer;
        this.behandleInngaaendeJournalConsumer = behandleInngaaendeJournalConsumer;
        this.journalConsumer = journalConsumer;
        this.periodeService = periodeService;
    }

    public void behandleJournalpost(String journalpostId) {
        Inntektsmelding inntektsmelding = getInntektsmelding(journalpostId);
        String saksId = periodeService.samleSaksinformasjon(inntektsmelding);
        InngaendeJournalpost journalpost = hentInngaendeJournalpost(saksId, inntektsmelding);
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(journalpost);
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost);
    }

    public InngaendeJournalpost hentInngaendeJournalpost(String gsakId, Inntektsmelding inntektsmelding) {
        String dokumentId = inngaaendeJournalConsumer.hentDokumentId(inntektsmelding.getJournalpostId());
        String geografiskTilknytning = personConsumer.hentGeografiskTilknytning(inntektsmelding.getFnr());
        String behandlendeEnhetId = arbeidsfordelingConsumer.finnBehandlendeEnhet(geografiskTilknytning);

        log.info("Hentet journalpost: ", inntektsmelding.getJournalpostId());

        return InngaendeJournalpost.builder()
                .fnr(inntektsmelding.getFnr())
                .gsakId(gsakId)
                .journalpostId(inntektsmelding.getJournalpostId())
                .dokumentId(dokumentId)
                .behandlendeEnhetId(behandlendeEnhetId)
                .arbeidsgiverOrgnummer(inntektsmelding.getArbeidsgiverOrgnummer())
                .build();
    }

    private Inntektsmelding getInntektsmelding(String journalpostId) {
        String dokumentId = inngaaendeJournalConsumer.hentDokumentId(journalpostId);
        return journalConsumer.hentInntektsmelding(journalpostId, dokumentId);
    }
}
