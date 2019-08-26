package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OppgavebehandlingConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public no.nav.tjeneste.virksomhet.oppgavebehandling.v3.binding.OppgavebehandlingV3 oppgavebehandlingV3(@Value("${servicegateway.url}") String serviceUrl) {
        return new no.nav.syfo.consumer.util.ws.WsClient<no.nav.tjeneste.virksomhet.oppgavebehandling.v3.binding.OppgavebehandlingV3>().createPort(serviceUrl, no.nav.tjeneste.virksomhet.oppgavebehandling.v3.binding.OppgavebehandlingV3.class, Collections.singletonList(new LogErrorHandler()));
    }
}
