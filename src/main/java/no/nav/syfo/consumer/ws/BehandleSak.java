package no.nav.syfo.consumer.ws;

import no.nav.syfo.ws.OnBehalfOfOutInterceptor;
import no.nav.tjeneste.virksomhet.behandlesak.v1.BehandleSakV1;
import no.nav.tjeneste.virksomhet.behandlesak.v1.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandlesak.v1.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.WSOpprettSakRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class BehandleSak {
    private static final Logger logger = LoggerFactory.getLogger(BehandleSak.class);

    private final BehandleSakV1 behandleSakV1;

    @Inject
    public BehandleSak(BehandleSakV1 behandleSakV1) {
        this.behandleSakV1 = behandleSakV1;
    }

    public String opprettSak(String fnr) {
        try {
            String sakId = behandleSakV1.opprettSak(new WSOpprettSakRequest()
                    .withSak(new WSSak()
                            .withSakstype(new WSSakstyper().withValue("GEN"))
                            .withFagomraade(new WSFagomraader().withValue("SYK"))
                            .withFagsystem(new WSFagsystemer().withValue("FS22"))
                            .withGjelderBrukerListe(new WSPerson().withIdent(fnr))
                    )
            ).getSakId();
            logger.info("Opprettet sak i GSAK: {}", sakId);
            return sakId;
        } catch (OpprettSakSakEksistererAllerede e) {
            logger.error("Sak finnes allerede", e);
            throw new RuntimeException("Sak finnes allerede", e);

        } catch (OpprettSakUgyldigInput e) {
            logger.error("Ugyldig input", e);
            throw new RuntimeException("Ugyldid input i sak", e);
        }
    }
}
