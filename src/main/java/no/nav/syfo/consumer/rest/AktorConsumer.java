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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

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

    public AktorConsumer(TokenConsumer tokenConsumer,
                         @Value("${srvsyfoinntektsmelding.username}") String username,
                         @Value("${aktoerregister.api.v1.url}") String url) {
        this.tokenConsumer = tokenConsumer;
        this.username = username;
        this.url = url;
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
        headers.set("Nav-Call-Id", MDCOperations.getFromMDC(MDC_CALL_ID)); // Nav-Call-Id skal fases ut.
        headers.set("Nav-Callid", MDCOperations.getFromMDC(MDC_CALL_ID));
        headers.set("Nav-Consumer-Id", username);
        headers.set("Nav-Personidenter", sokeIdent);

        final String uriString = UriComponentsBuilder.fromHttpUrl(url + "/identer")
                .queryParam("gjeldende", "true")
                .queryParam("identgruppe", identgruppe)
                .toUriString();

        final ResponseEntity<String> result = new RestTemplate().exchange(uriString, GET, new HttpEntity<>(headers), String.class);

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
    }
}

