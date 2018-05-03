package no.nav.syfo.consumer.ws;

import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.ArbeidsfordelingV1;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSEnhetsstatus;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSOrganisasjonsenhet;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.WSFinnBehandlendeEnhetListeRequest;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.WSFinnBehandlendeEnhetListeResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArbeidsfordelingConsumerTest {

    @Mock
    private ArbeidsfordelingV1 arbeidsfordelingV1;

    @InjectMocks
    private ArbeidsfordelingConsumer arbeidsfordelingConsumer;

    @Test
    public void finnBehandlendeEnhet() throws Exception {
        when(arbeidsfordelingV1.finnBehandlendeEnhetListe(any()))
                .thenReturn(new WSFinnBehandlendeEnhetListeResponse()
                        .withBehandlendeEnhetListe(new WSOrganisasjonsenhet()
                                .withStatus(WSEnhetsstatus.AKTIV)
                                .withEnhetId("enhetId")
                        )
                );
        ArgumentCaptor<WSFinnBehandlendeEnhetListeRequest> captor = ArgumentCaptor.forClass(WSFinnBehandlendeEnhetListeRequest.class);

        String behandlendeEnhet = arbeidsfordelingConsumer.finnBehandlendeEnhet("geografiskTilknytning");
        verify(arbeidsfordelingV1).finnBehandlendeEnhetListe(captor.capture());

        assertThat(behandlendeEnhet).isEqualTo("enhetId");
        assertThat(captor.getValue().getArbeidsfordelingKriterier().getGeografiskTilknytning().getValue()).isEqualTo("geografiskTilknytning");
        assertThat(captor.getValue().getArbeidsfordelingKriterier().getTema().getValue()).isEqualTo("SYK");
    }

}