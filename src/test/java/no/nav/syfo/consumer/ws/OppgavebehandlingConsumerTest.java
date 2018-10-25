package no.nav.syfo.consumer.ws;

import no.nav.syfo.domain.Oppgave;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.OppgavebehandlingV3;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgave;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgaveRequest;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgaveResponse;
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
public class OppgavebehandlingConsumerTest {
    @Mock
    private OppgavebehandlingV3 oppgavebehandlingV3;

    @InjectMocks
    private OppgavebehandlingConsumer oppgavebehandlingConsumer;

    @Test
    public void opprettOppgave() {
        when(oppgavebehandlingV3.opprettOppgave(any())).thenReturn(new WSOpprettOppgaveResponse().withOppgaveId("1234"));
        ArgumentCaptor<WSOpprettOppgaveRequest> captor = ArgumentCaptor.forClass(WSOpprettOppgaveRequest.class);

        String ansvarligEnhetId = "behandlendeenhet1234";
        Oppgave oppgave = Oppgave.builder()
                .beskrivelse("Beskriv beskriv")
                .saksnummer("gsak1234")
                .dokumentId("journalpost1234")
                .ansvarligEnhetId(ansvarligEnhetId)
                .aktivTil(LocalDate.of(2018, 1, 1))
                .build();
        oppgavebehandlingConsumer.opprettOppgave("12345678910", oppgave);

        verify(oppgavebehandlingV3).opprettOppgave(captor.capture());
        WSOpprettOppgave opprettOppgave = captor.getValue().getOpprettOppgave();

        assertThat(opprettOppgave.getBrukertypeKode()).isEqualTo("PERSON");
        assertThat(opprettOppgave.getOppgavetypeKode()).isEqualTo("INNT_SYK");
        assertThat(opprettOppgave.getFagomradeKode()).isEqualTo("SYK");
        assertThat(opprettOppgave.getUnderkategoriKode()).isEqualTo("SYK_SYK");
        assertThat(opprettOppgave.getPrioritetKode()).isEqualTo("NORM_SYK");
        assertThat(opprettOppgave.getBeskrivelse()).isEqualTo(oppgave.getBeskrivelse());
        assertThat(opprettOppgave.getAktivTil()).isEqualTo(oppgave.getAktivTil());
        assertThat(opprettOppgave.getAnsvarligEnhetId()).isEqualTo(ansvarligEnhetId);
        assertThat(opprettOppgave.getDokumentId()).isEqualTo(oppgave.getDokumentId());
        assertThat(opprettOppgave.getSaksnummer()).isEqualTo(oppgave.getSaksnummer());
        assertThat(opprettOppgave.getOppfolging()).isEqualTo("\nDu kan gi oss tilbakemelding på søknaden om sykepenger.\n" +
                "Gå til internettadresse: nav.no/digitalsykmelding/tilbakemelding");
    }
}
