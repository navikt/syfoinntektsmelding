package no.nav.syfo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RestTemplateConfig {
    @Bean
    fun basicAuthRestTemplate(
        @Value("\${srvsyfoinntektsmelding.username}") username: String,
        @Value("\${srvsyfoinntektsmelding.password}") password: String
    ): RestTemplate {
        return RestTemplateBuilder()
            .basicAuthentication(username, password)
            .build()
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}
