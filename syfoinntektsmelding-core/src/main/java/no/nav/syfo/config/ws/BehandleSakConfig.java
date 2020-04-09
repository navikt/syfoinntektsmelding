package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.behandlesak.v2.BehandleSakV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class BehandleSakConfig {
    @Bean
    @SuppressWarnings("unchecked")
    public BehandleSakV2 behandleSakV2(@Value("${virksomhet.behandlesak.v2.endpointurl}") String serviceUrl) {
        return new WsClient<BehandleSakV2>().createPort(serviceUrl, BehandleSakV2.class, Collections.singletonList(new LogErrorHandler()));
    }
}
