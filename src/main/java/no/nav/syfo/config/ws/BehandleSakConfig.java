package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.behandlesak.v1.BehandleSakV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class BehandleSakConfig {
    @Bean
    @SuppressWarnings("unchecked")
    public BehandleSakV1 behandleSakV1(@Value("${servicegateway.url}") String serviceUrl) {
        return new WsClient<BehandleSakV1>().createPort(serviceUrl, BehandleSakV1.class, Collections.singletonList(new LogErrorHandler()));
    }
}
