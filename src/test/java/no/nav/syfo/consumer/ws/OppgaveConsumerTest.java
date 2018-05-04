package no.nav.syfo.consumer.ws;

import no.nav.syfo.domain.Oppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.OppgaveV3;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.WSOppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.WSStatus;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.WSHentOppgaveRequest;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.WSHentOppgaveResponse;
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
public class OppgaveConsumerTest {
    @Mock
    private OppgaveV3 oppgaveV3;

    @InjectMocks
    private OppgaveConsumer oppgaveConsumer;

    @Test
    public void finnOppgave() throws Exception {
        when(oppgaveV3.hentOppgave(any())).thenReturn(new WSHentOppgaveResponse().withOppgave(new WSOppgave().withOppgaveId("1234").withStatus(new WSStatus().withKode("statuskode"))));
        ArgumentCaptor<WSHentOppgaveRequest> captor = ArgumentCaptor.forClass(WSHentOppgaveRequest.class);

        Oppgave oppgave = oppgaveConsumer.finnOppgave("1234").get();

        verify(oppgaveV3).hentOppgave(captor.capture());

        assertThat(oppgave.getOppgaveId()).isEqualTo("1234");
        assertThat(captor.getValue().getOppgaveId()).isEqualTo("1234");
    }

}