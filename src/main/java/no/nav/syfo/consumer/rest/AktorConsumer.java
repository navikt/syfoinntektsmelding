package no.nav.syfo.consumer.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

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
        headers.set("Nav-Call-Id", "syfo-" + UUID.randomUUID().toString()); // Nav-Call-Id skal fases ut.
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


            final JsonNode identNode = objectMapper
                    .readTree(result.getBody())
                    .at("/" + sokeIdent);

            if (identNode.isMissingNode()) {
                log.error("Fnr som ble slått opp på er ikke i responsen!");
                throw new RuntimeException("Fnr som ble slått opp på er ikke i responsen!");
            }

            AktorResponse aktor = objectMapper
                    .readerFor(new TypeReference<AktorResponse>() {
                    }).readValue(identNode);

            return Optional.ofNullable(aktor.getIdenter())
                    .orElseThrow(() -> {
                        log.error("Ident er null: " + aktor.getFeilmelding());
                        return new RuntimeException("Ident er null: " + aktor.getFeilmelding());
                    })
                    .stream()
                    .map(Ident::getIdent)
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("Ident er tom!");
                        return new RuntimeException("Ident er tom!");
                    });

        } catch (HttpClientErrorException e) {
            log.error("Feil ved oppslag i aktørtjenesten");
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Feil ved deserialisering av respons", e);
            throw new RuntimeException(e);
        }
    }
}

