package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.BehandleInngaaendeJournalV1;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BehandleInngaaendeJournalConsumer {

    private BehandleInngaaendeJournalV1 behandleInngaaendeJournalV1;

    public BehandleInngaaendeJournalConsumer(BehandleInngaaendeJournalV1 behandleInngaaendeJournalV1) {
        this.behandleInngaaendeJournalV1 = behandleInngaaendeJournalV1;
    }

    public void oppdaterJournalpost() {
    }
}
