package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.ArbeidsfordelingConsumer;
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.syfo.domain.Oppgave;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SaksbehandlingServiceTest {

    @Mock
    private PersonConsumer personConsumer;
    @Mock
    private ArbeidsfordelingConsumer arbeidsfordelingConsumer;
    @Mock
    private OppgavebehandlingConsumer oppgavebehandlingConsumer;

    @InjectMocks
    private SaksbehandlingService saksbehandlingService;

    @Test
    public void opprettOppgave() throws Exception {
        when(personConsumer.hentGeografiskTilknytning(anyString())).thenReturn("Geografisktilknytning");
        when(arbeidsfordelingConsumer.finnBehandlendeEnhet(anyString())).thenReturn("behandlendeenhet1234");
        when(oppgavebehandlingConsumer.opprettOppgave(anyString(), any())).thenReturn("1234");

        LocalDate aktivTil = LocalDate.of(2018, 1, 1);
        Oppgave oppgave = Oppgave.builder()
                .beskrivelse("Beskriv beskriv")
                .gsakSaksid("gsak1234")
                .journalpostId("journalpost1234")
                .aktivTil(aktivTil)
                .build();

        assertThat(saksbehandlingService.opprettOppgave("12345678910", oppgave).getOppgaveId()).isEqualTo("1234");
    }

}