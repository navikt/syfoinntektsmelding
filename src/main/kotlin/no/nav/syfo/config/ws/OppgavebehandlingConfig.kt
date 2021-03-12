package no.nav.syfo.config.ws

import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.binding.OppgavebehandlingV3
import no.nav.syfo.consumer.util.ws.WsClient
import no.nav.syfo.consumer.util.ws.LogErrorHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OppgavebehandlingConfig {
    @Bean
    fun oppgavebehandlingV3(@Value("\${servicegateway.url}") serviceUrl: String?): OppgavebehandlingV3 {
        return WsClient<OppgavebehandlingV3>().createPort(
            serviceUrl!!,
            OppgavebehandlingV3::class.java,
            listOf(LogErrorHandler())
        )
    }
}
