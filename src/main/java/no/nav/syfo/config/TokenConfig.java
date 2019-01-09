package no.nav.syfo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TokenConfig {

    @Bean
    public RestTemplate restTemplate(@Value("${srvsyfoinntektsmelding.username}") String username,
                              @Value("${srvsyfoinntektsmelding.password}") String password) {
        return new RestTemplateBuilder()
                .basicAuthorization(username, password)
                .build();
    }
}
