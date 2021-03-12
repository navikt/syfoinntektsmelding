package no.nav.syfo.config.ws

import no.nav.tjeneste.virksomhet.behandlesak.v2.BehandleSakV2
import no.nav.syfo.consumer.util.ws.WsClient
import no.nav.syfo.consumer.util.ws.LogErrorHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BehandleSakConfig {
    @Bean
    fun behandleSakV2(@Value("\${virksomhet.behandlesak.v2.endpointurl}") serviceUrl: String?): BehandleSakV2 {
        return WsClient<BehandleSakV2>().createPort(serviceUrl!!, BehandleSakV2::class.java, listOf(LogErrorHandler()))
    }
}
