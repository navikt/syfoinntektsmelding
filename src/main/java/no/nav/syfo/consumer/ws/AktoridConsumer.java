package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.WSHentIdentForAktoerIdRequest;
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

    public String hentFnrForAktorid(String aktorid) {
        try {
            return aktoerV2.hentIdentForAktoerId(new WSHentIdentForAktoerIdRequest().withAktoerId(aktorid)).getIdent();
        } catch (HentIdentForAktoerIdPersonIkkeFunnet e) {
            log.error("Fant ikke person med aktorid: {}", aktorid);
            throw new RuntimeException(e);
        }
    }
}
