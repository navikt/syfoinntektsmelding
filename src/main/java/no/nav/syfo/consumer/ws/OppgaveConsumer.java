package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Oppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.HentOppgaveOppgaveIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppgave.v3.OppgaveV3;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.WSOppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.WSHentOppgaveRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

import static org.jvnet.jaxb2_commons.lang.StringUtils.isEmpty;

@Component
@Slf4j
public class OppgaveConsumer {

    private final OppgaveV3 oppgaveV3;

    @Inject
    public OppgaveConsumer(OppgaveV3 oppgaveV3) {
        this.oppgaveV3 = oppgaveV3;
    }

    public Optional<Oppgave> finnOppgave(String oppgaveId) {
        if (isEmpty(oppgaveId)) {
            log.error("Prøvde å finne oppgave med tom oppgave id");
            throw new RuntimeException("Prøvde å finne oppgave med tom oppgave id");
        }

        try {
            WSOppgave wsOppgave = oppgaveV3.hentOppgave(new WSHentOppgaveRequest().withOppgaveId(oppgaveId)).getOppgave();
            Oppgave oppgave = Oppgave.builder()
                    .oppgaveId(wsOppgave.getOppgaveId())
                    .beskrivelse(wsOppgave.getBeskrivelse())
                    .gsakSaksid(wsOppgave.getSaksnummer())
                    .aktivTil(wsOppgave.getAktivTil())
                    .status(wsOppgave.getStatus().getKode())
                    .build();
            log.info("Hentet oppgave: {}", oppgaveId);
            return Optional.of(oppgave);
        } catch (HentOppgaveOppgaveIkkeFunnet e) {
            log.warn("Fant ikke oppgave med id {}", oppgaveId);
            return Optional.empty();
        }
    }
}
