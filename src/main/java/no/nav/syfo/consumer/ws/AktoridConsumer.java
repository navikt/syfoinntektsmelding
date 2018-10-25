package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentAktoerIdForIdentRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Slf4j
public class AktoridConsumer {

    private final AktoerV2 aktoerV2;

    @Inject
    public AktoridConsumer(AktoerV2 aktoerV2) {
        this.aktoerV2 = aktoerV2;
    }

    public String hentAktoerIdForFnr(String fnr) {
        try {
            return aktoerV2.hentAktoerIdForIdent(new WSHentAktoerIdForIdentRequest().withIdent(fnr)).getAktoerId();
        } catch (HentAktoerIdForIdentPersonIkkeFunnet e) {
            log.error("Fant ikke person med gitt fnr");
            throw new RuntimeException(e);
        }
    }
}
