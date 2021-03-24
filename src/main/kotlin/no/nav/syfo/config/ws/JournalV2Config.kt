package no.nav.syfo.config.ws

import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2
import no.nav.syfo.consumer.util.ws.WsClient
import no.nav.syfo.consumer.util.ws.LogErrorHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JournalV2Config {
    @Bean
    fun ç(@Value("\${journal.v2.endpointurl}") serviceUrl: String?): JournalV2 {
        return WsClient<JournalV2>().createPort(serviceUrl!!, JournalV2::class.java, listOf(LogErrorHandler()))
    }
}
