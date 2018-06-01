package no.nav.syfo.consumer.ws;

import no.nav.syfo.domain.Oppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.OppgaveV3;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.*;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.WSHentOppgaveRequest;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.WSHentOppgaveResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

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
        when(oppgaveV3.hentOppgave(any())).thenReturn(new WSHentOppgaveResponse()
                .withOppgave(new WSOppgave()
                        .withOppgaveId("1234")
                        .withVersjon(1)
                        .withBeskrivelse("beskrivelse")
                        .withAktivFra(LocalDate.of(2018, 5, 1))
                        .withAktivTil(LocalDate.of(2018, 5, 10))
                        .withOppgavetype(new WSOppgavetype().withKode("oppgavetype"))
                        .withFagomrade(new WSFagomrade().withKode("fagomrade"))
                        .withPrioritet(new WSPrioritet().withKode("prioritet"))
                        .withAnsvarligEnhetId("ansvarligenhetsid")
                        .withSaksnummer("saksnummer")
                        .withDokumentId("dokumentid")
                        .withStatus(new WSStatus().withKode("statuskode"))
                ));
        ArgumentCaptor<WSHentOppgaveRequest> captor = ArgumentCaptor.forClass(WSHentOppgaveRequest.class);

        Oppgave oppgave = oppgaveConsumer.finnOppgave("1234").get();

        verify(oppgaveV3).hentOppgave(captor.capture());

        assertThat(oppgave.getOppgaveId()).isEqualTo("1234");
        assertThat(captor.getValue().getOppgaveId()).isEqualTo("1234");
    }

}