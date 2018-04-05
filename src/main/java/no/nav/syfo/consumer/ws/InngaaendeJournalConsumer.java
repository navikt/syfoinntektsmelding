package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.InngaaendeJournalpost;
import no.nav.syfo.domain.SyfoException;
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.*;
import org.springframework.stereotype.Component;

import static no.nav.syfo.mappers.WS2InngaaendeJournalpostMapper.ws2InngaaendeJouralpost;
import static no.nav.syfo.util.MapUtil.map;

@Component
@Slf4j
public class InngaaendeJournalConsumer {

    private InngaaendeJournalV1 inngaaendeJournalV1;

    public InngaaendeJournalConsumer(InngaaendeJournalV1 inngaaendeJournalV1) {
        this.inngaaendeJournalV1 = inngaaendeJournalV1;
    }

    public InngaaendeJournalpost hentJournalpost(String journalpostId) {
        try {
            final WSInngaaendeJournalpost inngaaendeJournalpost = inngaaendeJournalV1.hentJournalpost(new WSHentJournalpostRequest().withJournalpostId(journalpostId)).getInngaaendeJournalpost();
            return map(inngaaendeJournalpost, ws2InngaaendeJouralpost);

        } catch (HentJournalpostSikkerhetsbegrensning e) {
            log.error("Feil ved henting av journalpost: Sikkerhetsbegrensning!");
            throw new SyfoException("Feil ved henting av journalpost: Sikkerhetsbegrensning!", e);
        } catch (HentJournalpostJournalpostIkkeInngaaende e) {
            log.error("Feil ved henting av journalpost: Journalpost er ikke inngaaende!");
            throw new SyfoException("Feil ved henting av journalpost: Journalpost er ikke inngaaende!", e);
        } catch (HentJournalpostJournalpostIkkeFunnet e) {
            log.error("Feil ved henting av journalpost: Journalpost ikke funnet!");
            throw new SyfoException("Feil ved henting av journalpost: Journalpost ikke funnet!", e);
        } catch (HentJournalpostUgyldigInput e) {
            log.error("Feil ved henting av journalpost: Journalpostid ikke gyldig!");
            throw new SyfoException("Feil ved henting av journalpost: Journalpostid ikke gyldig!", e);
        }
    }
}
