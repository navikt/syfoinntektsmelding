package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class PersonConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public PersonV3 personV3(@Value("${servicegateway.url}") String serviceUrl) {
        return new WsClient<PersonV3>().createPort(serviceUrl, PersonV3.class, Collections.singletonList(new LogErrorHandler()));
    }

}
