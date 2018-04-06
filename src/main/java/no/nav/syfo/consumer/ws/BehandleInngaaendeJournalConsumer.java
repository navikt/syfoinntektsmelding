package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.InngaaendeJournalpost;
import no.nav.syfo.domain.SyfoException;
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.BehandleInngaaendeJournalV1;
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.WSOppdaterJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.*;
import org.springframework.stereotype.Component;

import static no.nav.syfo.mappers.WS2InngaaendeJournalpostMapper.ws2InngaaendeJouralpost;
import static no.nav.syfo.util.MapUtil.map;

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
