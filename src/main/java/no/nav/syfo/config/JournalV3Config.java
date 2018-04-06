package no.nav.syfo.config;

import no.nav.syfo.ws.WsClient;
import no.nav.tjeneste.virksomhet.journal.v3.JournalV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JournalV3Config {

    @SuppressWarnings("unchecked")
    @Bean
    public JournalV3 journalV3(
            @Value("${journal.v3.endpointurl}") String serviceUrl) {
        return new WsClient<JournalV3>().createPort(serviceUrl, JournalV3.class);
    }
}
