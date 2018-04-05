package no.nav.syfo.config;

import no.nav.syfo.ws.WsClient;
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.InngaaendeJournalV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InngaaendeJournalV1Config {

    @SuppressWarnings("unchecked")
    @Bean
    public InngaaendeJournalV1 inngaaendeJournalV1(
            @Value("${inngaaendejournalv1.endpointurl}") String serviceUrl) {
        return new WsClient<InngaaendeJournalV1>().createPort(serviceUrl, InngaaendeJournalV1.class);
    }
}
