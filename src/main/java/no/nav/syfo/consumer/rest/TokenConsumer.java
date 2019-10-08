package no.nav.syfo.consumer.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
public class TokenConsumer {

    private RestTemplate basicAuthRestTemplate;
    private String url;

    public TokenConsumer(RestTemplate basicAuthRestTemplate,
                         @Value("${security-token-service-token.url}") String url) {
        this.basicAuthRestTemplate = basicAuthRestTemplate;
        this.url = url;
    }

    public String getToken() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        final String uriString = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("grant_type", "client_credentials")
                .queryParam("scope", "openid")
                .toUriString();

        final ResponseEntity<Token> result = basicAuthRestTemplate.exchange(uriString, GET, new HttpEntity<>(headers), Token.class);

        if (result.getStatusCode() != OK) {
            throw new RuntimeException("Henting av token feiler med HTTP-" + result.getStatusCode());
        }

        return requireNonNull(result.getBody()).getAccess_token();
    }
}

