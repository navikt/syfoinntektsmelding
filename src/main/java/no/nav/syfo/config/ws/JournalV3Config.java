package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.journal.v3.JournalV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class JournalV3Config {

    @SuppressWarnings("unchecked")
    @Bean
    public JournalV3 journalV3(@Value("${servicegateway.url}") String serviceUrl) {
        return new WsClient<JournalV3>().createPort(serviceUrl, JournalV3.class, Collections.singletonList(new LogErrorHandler()));
    }
}
