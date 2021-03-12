package no.nav.syfo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OppgaveClientConfigProvider {
    @Bean
    fun getOppgaveClientConfig(
        @Value("\${oppgavebehandling.url}") url: String,
        @Value("\${securitytokenservice.url}") tokenUrl: String,
        @Value("\${srvappserver.username}") username: String,
        @Value("\${srvappserver.password}") password: String
    ): OppgaveConfig {
        return OppgaveConfig(url, tokenUrl, username, password)
    }
}
