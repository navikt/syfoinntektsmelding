package no.nav.syfo.consumer.rest;

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

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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
        AktorResponse response = AktorResponse.builder().build();
        response.put("fnr", Aktor.builder()
                .identer(asList(Ident.builder()
                        .ident("aktorId")
                        .identgruppe("AktoerId")
                        .gjeldende(true)
                        .build()
                ))
                .feilmelding(null)
                .build());

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
        AktorResponse response = AktorResponse.builder().build();
        response.put("fnr", Aktor.builder()
                .identer(null)
                .feilmelding("Fant ikke aktør")
                .build());

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
        AktorResponse response = AktorResponse.builder().build();
        response.put("etAnnetFnr", Aktor.builder()
                .identer(asList(Ident.builder()
                        .ident("aktorId")
                        .identgruppe("AktoerId")
                        .gjeldende(true)
                        .build()
                ))
                .feilmelding(null)
                .build());


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
        AktorResponse response = AktorResponse.builder().build();
        response.put("fnr", Aktor.builder()
                .identer(Collections.emptyList())
                .feilmelding(null)
                .build());

        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                (Class<AktorResponse>) any())
        ).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));
        aktorConsumer.getAktorId("fnr");
    }
}
