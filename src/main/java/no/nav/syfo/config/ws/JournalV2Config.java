package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.journal.v2.JournalV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class JournalV2Config {

    @SuppressWarnings("unchecked")
    @Bean
    public JournalV2 journalV2(@Value("${servicegateway.url}") String serviceUrl) {
        return new WsClient<JournalV2>().createPort(serviceUrl, JournalV2.class, Collections.singletonList(new LogErrorHandler()));
    }
}
