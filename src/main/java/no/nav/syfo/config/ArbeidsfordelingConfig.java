package no.nav.syfo.config;

import no.nav.syfo.ws.WsClient;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.ArbeidsfordelingV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArbeidsfordelingConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public ArbeidsfordelingV1 arbeidsfordelingV1(@Value("${virksomhet.arbeidsfordeling.v1.endpointurl}") String serviceUrl) {
        return new WsClient<ArbeidsfordelingV1>().createPort(serviceUrl, ArbeidsfordelingV1.class);
    }
}
