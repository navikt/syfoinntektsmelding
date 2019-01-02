package no.nav.syfo.consumer.rest;


import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.util.MDCOperations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

import static no.nav.syfo.util.MDCOperations.MDC_CALL_ID;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
public class EksisterendeSakConsumer {

    private final TokenConsumer tokenConsumer;
    private final RestTemplate restTemplate;
    private final String stranglerUrl;

    public EksisterendeSakConsumer(TokenConsumer tokenConsumer, RestTemplate restTemplate, @Value("${syfoservicestranglerApi.url}") String stranglerUrl) {
        this.tokenConsumer = tokenConsumer;
        this.restTemplate = restTemplate;
        this.stranglerUrl = stranglerUrl;
    }

    public Optional<String> finnEksisterendeSaksId(String aktorId, String orgnummer) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + tokenConsumer.getToken());
        headers.set("Nav-Callid", MDCOperations.getFromMDC(MDC_CALL_ID));

        final String uriString = UriComponentsBuilder.fromHttpUrl(stranglerUrl + aktorId + "/soknader/nyesteSak")
                .queryParam("orgnummer", orgnummer)
                .toUriString();

        final ResponseEntity<NyesteSakResponse> result = restTemplate.exchange(uriString, GET, new HttpEntity<>(headers), NyesteSakResponse.class);

        if (result.getStatusCode() != OK) {
            final String message = "Kall mot aktørregister feiler med HTTP-" + result.getStatusCode();
            log.error(message);
            throw new RuntimeException(message);
        }

        return Optional.ofNullable(result.getBody().getNyesteSak());
    }
}
