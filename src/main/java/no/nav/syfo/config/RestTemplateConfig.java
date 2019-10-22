package no.nav.syfo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate basicAuthRestTemplate(@Value("${srvsyfoinntektsmelding.username}") String username,
                                              @Value("${srvsyfoinntektsmelding.password}") String password) {
        return new RestTemplateBuilder()
            .basicAuthentication(username, password)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
