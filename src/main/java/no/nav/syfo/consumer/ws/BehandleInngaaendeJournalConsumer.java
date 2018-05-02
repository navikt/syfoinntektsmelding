package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.InngaendeJournalpost;
import no.nav.syfo.domain.SyfoException;
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BehandleInngaaendeJournalConsumer {

    private BehandleInngaaendeJournalV1 behandleInngaaendeJournalV1;

    public BehandleInngaaendeJournalConsumer(BehandleInngaaendeJournalV1 behandleInngaaendeJournalV1) {
        this.behandleInngaaendeJournalV1 = behandleInngaaendeJournalV1;
    }

    public void oppdaterJournalpost(InngaendeJournalpost inngaendeJournalpost) {
        String journalpostId = inngaendeJournalpost.getJournalpostId();

        WSInngaaendeJournalpost inngaaendeJournalpost = new WSInngaaendeJournalpost()
                .withJournalpostId(journalpostId)
                .withBruker(new WSPerson().withIdent(inngaendeJournalpost.getFnr()))
                .withAvsender(new WSAvsender().withAvsenderNavn("Arbeidsgiver").withAvsenderId(inngaendeJournalpost.getArbeidsgiverOrgnummer()))
                .withArkivSak(new WSArkivSak().withArkivSakId(inngaendeJournalpost.getGsakId()).withArkivSakSystem("FS22"));

        try {
            behandleInngaaendeJournalV1.oppdaterJournalpost(new WSOppdaterJournalpostRequest()
                    .withInngaaendeJournalpost(inngaaendeJournalpost)
            );
            log.info("Oppdaterte journalpost: {}", journalpostId);
        } catch (OppdaterJournalpostUgyldigInput e) {
            log.error("Feil ved oppdatering av journalpost: {} - Ugyldig input!", journalpostId, e);
            throw new SyfoException("Feil ved oppdatering av journalpost: " + journalpostId + " - Ugyldig input!", e);
        } catch (OppdaterJournalpostObjektIkkeFunnet e) {
            log.error("Feil ved oppdatering av journalpost: {} - Journalpost ikke funnet!", journalpostId, e);
            throw new SyfoException("Feil ved oppdatering av journalpost: " + journalpostId + " Journalpost ikke funnet!", e);
        } catch (OppdaterJournalpostOppdateringIkkeMulig e) {
            log.error("Feil ved oppdatering av journalpost: {} - Oppdatering ikke mulig!", journalpostId, e);
            throw new SyfoException("Feil ved oppdatering av journalpost: " + journalpostId + " - Oppdatering ikke mulig!", e);
        } catch (OppdaterJournalpostJournalpostIkkeInngaaende e) {
            log.error("Feil ved oppdatering av journalpost: {} - Journalpost er ikke inngående!", journalpostId, e);
            throw new SyfoException("Feil ved oppdatering av journalpost: " + journalpostId + " - Journalpost er ikke inngående!", e);
        } catch (OppdaterJournalpostSikkerhetsbegrensning e) {
            log.error("Feil ved oppdatering av journalpost: {} - Sikkerhetsbegrensning!", journalpostId, e);
            throw new SyfoException("Feil ved oppdatering av journalpost: " + journalpostId + " - Sikkerhetsbegrensning!", e);
        }
    }

    public void ferdigstillJournalpost(InngaendeJournalpost inngaendeJournalpost) {
        String journalpostId = inngaendeJournalpost.getJournalpostId();
        try {
            behandleInngaaendeJournalV1.ferdigstillJournalfoering(new WSFerdigstillJournalfoeringRequest()
                    .withEnhetId(inngaendeJournalpost.getBehandlendeEnhetId())
                    .withJournalpostId(journalpostId)
            );
            log.info("Ferdigstiller journalpost med id: {}", journalpostId);
        } catch (FerdigstillJournalfoeringUgyldigInput e) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Ugyldig input!", journalpostId, e);
            throw new SyfoException("Feil ved ferdigstilling av journalpost: " + journalpostId + " - Ugyldig innput!", e);
        } catch (FerdigstillJournalfoeringObjektIkkeFunnet e) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Journalpost ikke funnet", journalpostId, e);
            throw new SyfoException("Feil ved ferdigstilling av journalpost: " + journalpostId + " - Journalpost ikke funnet!", e);
        } catch (FerdigstillJournalfoeringJournalpostIkkeInngaaende e) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Ikke inngående!", journalpostId, e);
            throw new SyfoException("Feil ved ferdigstilling av journalpost: " + journalpostId + " - Ikke inngående!", e);
        } catch (FerdigstillJournalfoeringSikkerhetsbegrensning e) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Sikkerhetsbegrensing!", journalpostId, e);
            throw new SyfoException("Feil ved ferdigstilling av journalpost: " + journalpostId + " - Sikkerhetsbegrensing!", e);
        } catch (FerdigstillJournalfoeringFerdigstillingIkkeMulig e) {
            log.error("Feil ved ferdigstilling av journalpost: {} - Ikke mulig å ferdigstille!", journalpostId, e);
            throw new SyfoException("Feil ved ferdigstilling av journalpost: " + journalpostId + " - Ikke mulig å ferdigstille!", e);
        }
    }
}
