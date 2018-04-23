package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.OppgavebehandlingV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OppgavebehandlingConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public OppgavebehandlingV3 oppgavebehandlingV3(@Value("${virksomhet.oppgavebehandling.v3.endpointurl}") String serviceUrl) {
        return new WsClient<OppgavebehandlingV3>().createPort(serviceUrl, OppgavebehandlingV3.class, Collections.singletonList(new LogErrorHandler()));
    }
}
