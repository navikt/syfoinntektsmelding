package no.nav.syfo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PDLClientConfigProvider {

    @Bean
    public PDLConfig getPDLClientConfig(
        @Value("${pdl.url}")
            String url,
        @Value("${securitytokenservice.url}")
            String tokenUrl,
        @Value("${srvappserver.username}")
            String username,
        @Value("${srvappserver.password}")
            String password) {
        return new PDLConfig(url, tokenUrl, username, password);
    }

}
