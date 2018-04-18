package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.behandlesak.v1.BehandleSakV1;
import no.nav.tjeneste.virksomhet.oppgave.v3.OppgaveV3;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.OppgavebehandlingV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaksbehandlingConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public BehandleSakV1 behandleSakV1(@Value("${virksomhet.behandlesak.v1.endpointurl}") String serviceUrl) {
        return new WsClient<BehandleSakV1>().createPort(serviceUrl, BehandleSakV1.class);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public OppgaveV3 oppgaveV3(@Value("${virksomhet.oppgave.v3.endpointurl}") String serviceUrl) {
        return new WsClient<OppgaveV3>().createPort(serviceUrl, OppgaveV3.class);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public OppgavebehandlingV3 oppgavebehandlingV3(@Value("${virksomhet.oppgavebehandling.v3.endpointurl}") String serviceUrl) {
        return new WsClient<OppgavebehandlingV3>().createPort(serviceUrl, OppgavebehandlingV3.class);
    }
}
