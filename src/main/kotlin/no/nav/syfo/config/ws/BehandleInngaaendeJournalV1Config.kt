package no.nav.syfo.config.ws

import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.BehandleInngaaendeJournalV1
import no.nav.syfo.consumer.util.ws.WsClient
import no.nav.syfo.consumer.util.ws.LogErrorHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BehandleInngaaendeJournalV1Config {
    @Bean
    fun behandleInngaaendeJournalV1(@Value("\${behandleinngaaendejournal.v1.endpointurl}") serviceUrl: String?): BehandleInngaaendeJournalV1 {
        return WsClient<BehandleInngaaendeJournalV1>().createPort(
            serviceUrl!!,
            BehandleInngaaendeJournalV1::class.java,
            listOf(LogErrorHandler())
        )
    }
}
