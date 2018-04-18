package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.oppgave.v3.OppgaveV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OppgaveConfig {
    @Bean
    @SuppressWarnings("unchecked")
    public OppgaveV3 oppgaveV3(@Value("${virksomhet.oppgave.v3.endpointurl}") String serviceUrl) {
        return new WsClient<OppgaveV3>().createPort(serviceUrl, OppgaveV3.class);
    }
}
