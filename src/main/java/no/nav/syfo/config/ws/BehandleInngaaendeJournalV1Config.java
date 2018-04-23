package no.nav.syfo.config.ws;

import no.nav.syfo.consumer.util.ws.LogErrorHandler;
import no.nav.syfo.consumer.util.ws.WsClient;
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.BehandleInngaaendeJournalV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class BehandleInngaaendeJournalV1Config {

    @SuppressWarnings("unchecked")
    @Bean
    public BehandleInngaaendeJournalV1 behandleInngaaendeJournalV1(
            @Value("${behandleinngaaendejournal.v1.endpointurl}") String serviceUrl) {
        return new WsClient<BehandleInngaaendeJournalV1>().createPort(serviceUrl, BehandleInngaaendeJournalV1.class, Collections.singletonList(new LogErrorHandler()));
    }
}
