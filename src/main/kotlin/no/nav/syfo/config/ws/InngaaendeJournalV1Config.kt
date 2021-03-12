package no.nav.syfo.config.ws

import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.InngaaendeJournalV1
import no.nav.syfo.consumer.util.ws.WsClient
import no.nav.syfo.consumer.util.ws.LogErrorHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InngaaendeJournalV1Config {
    @Bean
    fun inngaaendeJournalV1(@Value("\${inngaaendejournal.v1.endpointurl}") serviceUrl: String?): InngaaendeJournalV1 {
        return WsClient<InngaaendeJournalV1>().createPort(
            serviceUrl!!,
            InngaaendeJournalV1::class.java,
            listOf(LogErrorHandler())
        )
    }
}
