package no.nav.syfo.config;

import no.nav.syfo.ws.WsClient;
import no.nav.tjeneste.virksomhet.behandlesak.v1.BehandleSakV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BehandleSakConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public BehandleSakV1 behandleSakV1(@Value("${virksomhet.behandlesak.v1.endpoint.url}") String serviceUrl) {
        return new WsClient<BehandleSakV1>().createPort(serviceUrl, BehandleSakV1.class);
    }
}
