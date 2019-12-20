package no.nav.syfo.consumer.rest.aktor;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.api.AktørKallResponseException;
import no.nav.syfo.api.AktørOppslagException;
import no.nav.syfo.api.AktorConsumerException;
import no.nav.syfo.api.FantIkkeAktørException;
import no.nav.syfo.api.TomAktørListeException;
import no.nav.syfo.consumer.rest.TokenConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

import static no.nav.syfo.util.MDCOperations.MDC_CALL_ID;
import static no.nav.syfo.util.MDCOperations.getFromMDC;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
public class AktorConsumer {

    private TokenConsumer tokenConsumer;
    private String username;
    private String url;
    private final RestTemplate restTemplate;

    public AktorConsumer(TokenConsumer tokenConsumer,
                         @Value("${srvsyfoinntektsmelding.username}") String username,
                         @Value("${aktoerregister.api.v1.url}") String url,
                         RestTemplate restTemplate) {
        this.tokenConsumer = tokenConsumer;
        this.username = username;
        this.url = url;
        this.restTemplate = restTemplate;
    }

    public String getAktorId(String fnr) throws AktorConsumerException {
        return getIdent(fnr, "AktoerId");
    }

    private String getIdent(String sokeIdent, String identgruppe) throws AktorConsumerException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + tokenConsumer.getToken());
        headers.set("Nav-Call-Id", getFromMDC(MDC_CALL_ID));
        headers.set("Nav-Consumer-Id", username);
        headers.set("Nav-Personidenter", sokeIdent);

        final String uriString = UriComponentsBuilder.fromHttpUrl(url + "/identer")
                .queryParam("gjeldende", "true")
                .queryParam("identgruppe", identgruppe)
                .toUriString();
        try {
            final ResponseEntity<AktorResponse> result = restTemplate.exchange(uriString, GET, new HttpEntity<>(headers), AktorResponse.class);

            if (result.getStatusCode() != OK) {
                final String message = "Kall mot aktørregister feiler med HTTP-" + result.getStatusCode();
                log.error(message);
                throw new AktørKallResponseException(sokeIdent, result.getStatusCode().value());
            }

            return Optional.of(result)
                    .map(HttpEntity::getBody)
                    .map(body -> body.get(sokeIdent))
                    .map(aktor -> Optional.ofNullable(aktor.getIdenter()).orElseThrow(() -> {
                        log.error("Fant ikke aktøren: " + aktor.getFeilmelding());
                        return new FantIkkeAktørException(sokeIdent);
                    }))
                    .flatMap(idents -> idents.stream().map(Ident::getIdent)
                            .findFirst())
                    .orElseThrow(() -> {
                        log.error("Feil ved henting av aktorId");
                        return new TomAktørListeException(sokeIdent);
                    });

        } catch (HttpClientErrorException e) {
            log.error("Feil ved oppslag i aktørtjenesten");
            throw new AktørOppslagException(sokeIdent, e);
        }
    }
}

