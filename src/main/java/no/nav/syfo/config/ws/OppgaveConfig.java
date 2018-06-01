package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.oppgave.v3.OppgaveV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OppgaveConfig {
    @Bean
    @SuppressWarnings("unchecked")
    public OppgaveV3 oppgaveV3(@Value("${servicegateway.url}") String serviceUrl) {
        return new WsClient<OppgaveV3>().createPort(serviceUrl, OppgaveV3.class, Collections.singletonList(new LogErrorHandler()));
    }
}
