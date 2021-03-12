package no.nav.syfo.rest;

import no.nav.syfo.consumer.rest.TokenConsumer;
import no.nav.syfo.consumer.rest.aktor.Aktor;
import no.nav.syfo.consumer.rest.aktor.AktorConsumer;
import no.nav.syfo.consumer.rest.aktor.AktorResponse;
import no.nav.syfo.consumer.rest.aktor.Ident;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class AktorConsumerTest {

    @Mock
    private TokenConsumer tokenConsumer;

    @Mock
    private RestTemplate restTemplate;

    private AktorConsumer aktorConsumer;

    @Before
    public void setup() {
        this.aktorConsumer = new AktorConsumer(tokenConsumer, "username", "https://aktor.nav.no", restTemplate);
        when(tokenConsumer.getToken()).thenReturn("token");
    }

    @Test
    public void finnerAktorId() {
        AktorResponse response = new AktorResponse();
        response.put("fnr", new Aktor(Arrays.asList(
            new Ident("aktorId","AktoerId", true )),
            null));
        when(restTemplate.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            (Class<AktorResponse>) any()
        ))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        String aktorId = aktorConsumer.getAktorId("fnr");
        assertThat(aktorId).isEqualTo("aktorId");
    }

    @Test(expected = RuntimeException.class)
    public void finnerIkkeIdent() {
        AktorResponse response = new AktorResponse();
        response.put("fnr",  new Aktor(null, "Fant ikke aktør"));

        when(restTemplate.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            (Class<AktorResponse>) any())
        ).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        aktorConsumer.getAktorId("fnr");
    }

    @Test(expected = RuntimeException.class)
    public void manglendeFnrIResponsGirFeilmelding() {
        AktorResponse response = new AktorResponse();
        response.put("etAnnetFnr", new Aktor(Arrays.asList(
            new Ident("aktorId","AktoerId", true )),
            null));

        when(restTemplate.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            (Class<AktorResponse>) any())
        ).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        aktorConsumer.getAktorId("fnr");
    }

    @Test(expected = RuntimeException.class)
    public void manglendeIdentGirFeilmelding() {
        AktorResponse response = new AktorResponse();
        response.put("fnr", new Aktor(null, null));

        when(restTemplate.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            (Class<AktorResponse>) any())
        ).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        aktorConsumer.getAktorId("fnr");
    }
}
