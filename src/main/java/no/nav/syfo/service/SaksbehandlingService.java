package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.ArbeidsfordelingConsumer;
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.domain.Oppgave;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Slf4j
public class SaksbehandlingService {
    private final OppgavebehandlingConsumer oppgavebehandlingConsumer;
    private final PersonConsumer personConsumer;
    private final ArbeidsfordelingConsumer arbeidsfordelingConsumer;

    @Inject
    public SaksbehandlingService(
            OppgavebehandlingConsumer oppgavebehandlingConsumer,
            ArbeidsfordelingConsumer arbeidsfordelingConsumer,
            PersonConsumer personConsumer
    ) {
        this.oppgavebehandlingConsumer = oppgavebehandlingConsumer;
        this.personConsumer = personConsumer;
        this.arbeidsfordelingConsumer = arbeidsfordelingConsumer;
    }

    public String opprettOppgave(String fnr, Oppgave oppgave) {
        String geografiskTilknytning = personConsumer.hentGeografiskTilknytning(fnr);
        String behandlendeEnhetId = arbeidsfordelingConsumer.finnBehandlendeEnhet(geografiskTilknytning);
        return oppgavebehandlingConsumer.opprettOppgave(fnr, behandlendeEnhetId, oppgave);
    }
}
