package no.nav.syfo.consumer.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.util.MDCOperations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static no.nav.syfo.util.MDCOperations.MDC_CALL_ID;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
public class AktorConsumer {

    private TokenConsumer tokenConsumer;
    private String username;
    private String url;
    private ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public AktorConsumer(TokenConsumer tokenConsumer,
                         @Value("${srvsyfoinntektsmelding.username}") String username,
                         @Value("${aktoerregister.api.v1.url}") String url, RestTemplate restTemplate) {
        this.tokenConsumer = tokenConsumer;
        this.username = username;
        this.url = url;
        this.restTemplate = restTemplate;
    }

    public String getAktorId(String fnr) {
        return getIdent(fnr, "AktoerId");
    }

    public String getFnr(String aktorId) {
        return getIdent(aktorId, "NorskIdent");
    }

    private String getIdent(String sokeIdent, String identgruppe) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + tokenConsumer.getToken());
        headers.set("Nav-Call-Id", "syfo-" + UUID.randomUUID()); // Nav-Call-Id skal fases ut.
        headers.set("Nav-Consumer-Id", username);
        headers.set("Nav-Personidenter", sokeIdent);

        final String uriString = UriComponentsBuilder.fromHttpUrl(url + "/identer")
                .queryParam("gjeldende", "true")
                .queryParam("identgruppe", identgruppe)
                .toUriString();
        try {
            final ResponseEntity<String> result = restTemplate.exchange(uriString, GET, new HttpEntity<>(headers), String.class);

            if (result.getStatusCode() != OK) {
                final String message = "Kall mot aktørregister feiler med HTTP-" + result.getStatusCode();
                log.error(message);
                throw new RuntimeException(message);
            }

            try {
                final JsonNode jsonNode = objectMapper
                        .readTree(result.getBody())
                        .at("/" + sokeIdent + "/identer");

                List<Ident> identer = objectMapper
                        .readerFor(new TypeReference<ArrayList<Ident>>() {
                        }).readValue(jsonNode);

                return identer.stream()
                        .map(Ident::getIdent)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Finner ikke ident"));
            } catch (Exception exception) {
                final String message = "Uventet feil ved henting av ident fra aktørregister";
                log.error(message);
                throw new RuntimeException(message, exception);
            }

        } catch (HttpClientErrorException e) {
            log.error(e.getMessage());
            log.error(e.getResponseBodyAsString());
            log.error("Uventet feil ved henting av ident fra aktørregister");
            throw new RuntimeException(e);
        }
    }
}

