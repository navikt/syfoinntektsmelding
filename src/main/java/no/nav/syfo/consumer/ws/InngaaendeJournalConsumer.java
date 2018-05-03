package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InngaaendeJournalConsumer {

    private InngaaendeJournalV1 inngaaendeJournalV1;

    public InngaaendeJournalConsumer(InngaaendeJournalV1 inngaaendeJournalV1) {
        this.inngaaendeJournalV1 = inngaaendeJournalV1;
    }

    public String hentDokumentId(String journalpostId) {
        try {
            final WSInngaaendeJournalpost inngaendeJournalpost = inngaaendeJournalV1.hentJournalpost(new WSHentJournalpostRequest().withJournalpostId(journalpostId)).getInngaaendeJournalpost();
            log.info("Hentet dokumentid: {}", inngaendeJournalpost.getHoveddokument().getDokumentId());
            return inngaendeJournalpost.getHoveddokument().getDokumentId();

        } catch (HentJournalpostSikkerhetsbegrensning e) {
            log.error("Feil ved henting av journalpost: Sikkerhetsbegrensning!");
            throw new RuntimeException("Feil ved henting av journalpost: Sikkerhetsbegrensning!", e);
        } catch (HentJournalpostJournalpostIkkeInngaaende e) {
            log.error("Feil ved henting av journalpost: Journalpost er ikke inngaaende!");
            throw new RuntimeException("Feil ved henting av journalpost: Journalpost er ikke inngaaende!", e);
        } catch (HentJournalpostJournalpostIkkeFunnet e) {
            log.error("Feil ved henting av journalpost: Journalpost ikke funnet!");
            throw new RuntimeException("Feil ved henting av journalpost: Journalpost ikke funnet!", e);
        } catch (HentJournalpostUgyldigInput e) {
            log.error("Feil ved henting av journalpost: Journalpostid ikke gyldig!");
            throw new RuntimeException("Feil ved henting av journalpost: Journalpostid ikke gyldig!", e);
        }
    }
}
