package no.nav.syfo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OppgaveClientConfigProvider {

    @Bean
    public OppgaveConfig getOppgaveClientConfig(
        @Value("${oppgavebehandling.url}")
        String url,
        @Value("${securitytokenservice.url}")
        String tokenUrl,
        @Value("${srvappserver.username}")
        String username,
        @Value("${srvappserver.password}")
        String password) {
        return new OppgaveConfig(url, tokenUrl, username, password);
    }

}
