package no.nav.syfo.consumer.ws;

import no.nav.syfo.util.Metrikk;
import no.nav.tjeneste.virksomhet.behandlesak.v1.BehandleSakV1;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.WSPerson;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.WSSak;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.WSOpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.WSOpprettSakResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class BehandleSakConsumerTest {
    @Mock
    private BehandleSakV1 behandleSakV1;

    @Mock
    private Metrikk metrikk;

    @InjectMocks
    private BehandleSakConsumer behandleSakConsumer;

    @Test
    public void opprettSak() throws Exception {
        when(behandleSakV1.opprettSak(any())).thenReturn(new WSOpprettSakResponse().withSakId("1"));
        ArgumentCaptor<WSOpprettSakRequest> captor = ArgumentCaptor.forClass(WSOpprettSakRequest.class);

        String sakId = behandleSakConsumer.opprettSak("12345678910");

        verify(behandleSakV1).opprettSak(captor.capture());
        WSSak sak = captor.getValue().getSak();

        assertThat(sakId).isEqualTo("1");
        assertThat(sak.getFagomraade().getValue()).isEqualTo("SYK");
        assertThat(sak.getFagsystem().getValue()).isEqualTo("FS22");
        assertThat(sak.getSakstype().getValue()).isEqualTo("GEN");
        assertThat(sak.getGjelderBrukerListe()).contains(new WSPerson().withIdent("12345678910"));
    }
}
