package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.oppgave.v3.HentOppgaveOppgaveIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppgave.v3.OppgaveV3;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.WSOppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.WSHentOppgaveRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static org.jvnet.jaxb2_commons.lang.StringUtils.isEmpty;

@Component
@Slf4j
public class OppgaveConsumer {

    private final OppgaveV3 oppgaveV3;

    @Inject
    public OppgaveConsumer(OppgaveV3 oppgaveV3) {
        this.oppgaveV3 = oppgaveV3;
    }

    public WSOppgave finnOppgave(String oppgaveId) {
        if (isEmpty(oppgaveId)) {
            return null;
        }

        try {
            WSOppgave oppgave = oppgaveV3.hentOppgave(new WSHentOppgaveRequest().withOppgaveId(oppgaveId)).getOppgave();
            log.info("Hentet oppgave: {}", oppgaveId);
            return oppgave;
        } catch (HentOppgaveOppgaveIkkeFunnet e) {
            log.warn("Fant ikke oppgave med id " + oppgaveId);
            return null;
        }
    }
}
