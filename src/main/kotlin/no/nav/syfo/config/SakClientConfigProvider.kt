package no.nav.syfo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SakClientConfigProvider {
    @Bean
    fun getSakClientConfig(
        @Value("\${opprett_sak_url}") url: String,
        @Value("\${securitytokenservice.url}") tokenUrl: String,
        @Value("\${srvappserver.username}") username: String,
        @Value("\${srvappserver.password}") password: String
    ): SakClientConfig {
        return SakClientConfig(url, tokenUrl, username, password)
    }
}
