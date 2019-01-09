package no.nav.syfo.consumer.rest;

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
        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                (Class<Object>) any())
        ).thenReturn(new ResponseEntity<>("{\"fnr\":{\"identer\":[\n" +
                "      {\n" +
                "        \"ident\": \"aktorId\",\n" +
                "        \"identgruppe\": \"AktoerId\",\n" +
                "        \"gjeldende\": true\n" +
                "      }\n" +
                "    ],\"feilmelding\":null}}", HttpStatus.OK));
        String aktorId = aktorConsumer.getAktorId("fnr");
        assertThat(aktorId).isEqualTo("aktorId");
    }

    @Test(expected = RuntimeException.class)
    public void finnerIkkeIdent() {
        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                (Class<Object>) any())
        ).thenReturn(new ResponseEntity<>("{\"fnr\":{\"identer\":null,\"feilmelding\":\"Den angitte personidenten finnes ikke\"}}", HttpStatus.OK));
        aktorConsumer.getAktorId("fnr");
    }

    @Test(expected = RuntimeException.class)
    public void manglendeFnrIResponsGirFeilmelding() {
        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                (Class<Object>) any())
        ).thenReturn(new ResponseEntity<>("{\"ikkeFr\":{\"identer\":[\n" +
                "      {\n" +
                "        \"ident\": \"aktorId\",\n" +
                "        \"identgruppe\": \"AktoerId\",\n" +
                "        \"gjeldende\": true\n" +
                "      }\n" +
                "    ],\"feilmelding\":null}}", HttpStatus.OK));
        aktorConsumer.getAktorId("fnr");
    }

    @Test(expected = RuntimeException.class)
    public void manglendeIdentGirFeilmelding() {
        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                (Class<Object>) any())
        ).thenReturn(new ResponseEntity<>("{\"fnr\":{\"identer\":[],\"feilmelding\":null}}", HttpStatus.OK));
        aktorConsumer.getAktorId("fnr");
    }


}
