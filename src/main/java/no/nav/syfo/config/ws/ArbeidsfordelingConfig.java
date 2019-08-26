package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class ArbeidsfordelingConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1 arbeidsfordelingV1(@Value("${virksomhet.arbeidsfordeling.v1.endpointurl}") String serviceUrl) {
        return new WsClient<no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1>().createPort(serviceUrl, no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1.class, Collections.singletonList(new LogErrorHandler()));
    }
}
