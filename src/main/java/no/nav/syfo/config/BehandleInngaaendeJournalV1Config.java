package no.nav.syfo.config;

import no.nav.syfo.ws.WsClient;
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.BehandleInngaaendeJournalV1;
import no.nav.tjeneste.virksomhet.inngaaende.journal.v1.InngaaendeJournalV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BehandleInngaaendeJournalV1Config {

    @SuppressWarnings("unchecked")
    @Bean
    public BehandleInngaaendeJournalV1 behandleInngaaendeJournalV1(
            @Value("${behandleinngaaendejournal.v1.endpointurl}") String serviceUrl) {
        return new WsClient<BehandleInngaaendeJournalV1>().createPort(serviceUrl, BehandleInngaaendeJournalV1.class);
    }
}
