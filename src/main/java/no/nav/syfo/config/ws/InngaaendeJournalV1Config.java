package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.InngaaendeJournalV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class InngaaendeJournalV1Config {

    @SuppressWarnings("unchecked")
    @Bean
    public InngaaendeJournalV1 inngaaendeJournalV1(
            @Value("${inngaaendejournal.v1.endpointurl}") String serviceUrl) {
        return new WsClient<InngaaendeJournalV1>().createPort(serviceUrl, InngaaendeJournalV1.class, Collections.singletonList(new LogErrorHandler()));
    }
}
