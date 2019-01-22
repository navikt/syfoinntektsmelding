package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.util.Metrikk;
import no.nav.tjeneste.virksomhet.behandlesak.v1.BehandleSakV1;
import no.nav.tjeneste.virksomhet.behandlesak.v1.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandlesak.v1.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.WSOpprettSakRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Slf4j
public class BehandleSakConsumer {

    private final BehandleSakV1 behandleSakV1;
    private final Metrikk metrikk;

    @Inject
    public BehandleSakConsumer(BehandleSakV1 behandleSakV1, Metrikk metrikk) {
        this.behandleSakV1 = behandleSakV1;
        this.metrikk = metrikk;
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

            log.info("Opprettet ny sak");
            metrikk.tellInntektsmeldingNySak();
            return sakId;
        } catch (OpprettSakSakEksistererAllerede e) {
            log.error("Sak finnes allerede", e);
            throw new RuntimeException("Sak finnes allerede", e);
        } catch (OpprettSakUgyldigInput e) {
            log.error("Ugyldig input", e);
            throw new RuntimeException("Ugyldid input i sak", e);
        }
    }
}
