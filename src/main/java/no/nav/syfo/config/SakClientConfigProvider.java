package no.nav.syfo.config;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;

@Configuration
public class SakClientConfigProvider {

    @Bean
    public SakClientConfig getSakClientConfig(
        @Value("${opprett_sak_url}")
            String url,
        @Value("${securitytokenservice.url}")
            String tokenUrl,
        @Value("${srvsyfoinntektsmelding.username}")
            String username,
        @Value("${srvsyfoinntektsmelding.password}")
            String password) {
        return new SakClientConfig(url, tokenUrl, username, password);
    }

}
