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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EksisterendeSakConsumerTest {

    @Mock
    private TokenConsumer tokenConsumer;

    @Mock
    private RestTemplate restTemplate;

    private EksisterendeSakConsumer eksisterendeSakConsumer;

    @Before
    public void setup() {
        eksisterendeSakConsumer = new EksisterendeSakConsumer(tokenConsumer, restTemplate, "http://vg.no");
    }

    @Test
    public void test() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), (Class<Object>) any())).thenReturn(new ResponseEntity<>("", HttpStatus.OK));

        eksisterendeSakConsumer.finnEksisterendeSaksId("123", "orgnummer");
    }

}
