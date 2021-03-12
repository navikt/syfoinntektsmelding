package no.nav.syfo.config.ws

import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.syfo.consumer.util.ws.WsClient
import no.nav.syfo.consumer.util.ws.LogErrorHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ArbeidsfordelingConfig {
    @Bean
    fun arbeidsfordelingV1(@Value("\${virksomhet.arbeidsfordeling.v1.endpointurl}") serviceUrl: String?): ArbeidsfordelingV1 {
        return WsClient<ArbeidsfordelingV1>().createPort(
            serviceUrl!!,
            ArbeidsfordelingV1::class.java,
            listOf(LogErrorHandler())
        )
    }
}
