package no.nav.syfo.service;

import no.nav.syfo.consumer.ws.BehandleSakConsumer;
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer;
import no.nav.syfo.consumer.ws.OppgaveConsumer;
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.repository.SykepengesoknadDAO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SaksbehandlingServiceTest {


    @Mock
    private OppgavebehandlingConsumer oppgavebehandlingConsumer;

    @Mock
    private BehandlendeEnhetConsumer behandlendeEnhetConsumer;

    @Mock
    private BehandleSakConsumer behandleSakConsumer;

    @Mock
    private OppgaveConsumer oppgaveConsumer;

    @Mock
    private SykepengesoknadDAO sykepengesoknadDAO;

    @InjectMocks
    private SaksbehandlingService saksbehandlingService;

    @Before
    public void setup() {
        when(behandlendeEnhetConsumer.hentGeografiskTilknytning(anyString())).thenReturn("Geografisktilknytning");
        when(behandlendeEnhetConsumer.hentBehandlendeEnhet(anyString())).thenReturn("behandlendeenhet1234");
        when(oppgavebehandlingConsumer.opprettOppgave(anyString(), any())).thenReturn("1234");
        when(behandleSakConsumer.opprettSak("fnr")).thenReturn("opprettetSaksId");
        when(oppgaveConsumer.finnOppgave("oppgaveId")).thenReturn(Optional.of(Oppgave.builder()
                .status("APEN")
                .beskrivelse("Beskriv beskriv")
                .gsakSaksid("gsak1234")
                .journalpostId("journalpost1234")
                .behandlendeEnhetId("behandlendeEnhet1234")
                .aktivTil(LocalDate.of(2018, 1, 1))
                .build()));
        when(sykepengesoknadDAO.hentSykepengesoknad("journalpostId")).thenReturn(
                Optional.of(Sykepengesoknad.builder()
                        .status("TIL_SENDING")
                        .journalpostId(null)
                        .oppgaveId("oppgaveId")
                        .saksId(null)
                        .uuid("uuid").build()));
    }

    @Test
    public void returnererSaksIdOmSakFinnes() {
        when(sykepengesoknadDAO.hentSykepengesoknad("journalpostId")).thenReturn(
                Optional.of(Sykepengesoknad.builder()
                                .status("SENDT")
                                .journalpostId("journalpostId")
                                .oppgaveId("oppgaveid-1")
                                .saksId("saksId")
                                .uuid("uuid-1").build()));

        Inntektsmelding inntektsmelding = Inntektsmelding.builder().fnr("fnr").arbeidsgiverOrgnummer("orgnummer").journalpostId("journalpostId").build();
        String saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding);

        assertThat(saksId).isEqualTo("saksId");
    }

    @Test
    public void oppretterSakOmSakIkkeFinnes() {
        when(sykepengesoknadDAO.hentSykepengesoknad("journalpostId")).thenReturn(
                Optional.of(Sykepengesoknad.builder()
                        .status("NY")
                        .journalpostId(null)
                        .oppgaveId(null)
                        .saksId(null)
                        .uuid("uuid").build()));

        Inntektsmelding inntektsmelding = Inntektsmelding.builder().fnr("fnr").arbeidsgiverOrgnummer("orgnummer").journalpostId("journalpostId").build();
        String saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding);

        assertThat(saksId).isEqualTo("opprettetSaksId");
    }

    @Test
    public void oppdatererOppgaveVedEksisterendeOppgave() {
        Inntektsmelding inntektsmelding = Inntektsmelding.builder().fnr("fnr").arbeidsgiverOrgnummer("orgnummer").journalpostId("journalpostId").build();
        saksbehandlingService.behandleInntektsmelding(inntektsmelding);

        verify(oppgavebehandlingConsumer, never()).opprettOppgave(anyString(), any(Oppgave.class));
        verify(oppgavebehandlingConsumer).oppdaterOppgavebeskrivelse(any(Oppgave.class), anyString());
    }

    @Test
    public void oppretterOppgaveVedFravaerendeOppgave() {
        when(oppgaveConsumer.finnOppgave("oppgaveId")).thenReturn(Optional.empty());

        Inntektsmelding inntektsmelding = Inntektsmelding.builder().fnr("fnr").arbeidsgiverOrgnummer("orgnummer").journalpostId("journalpostId").build();
        saksbehandlingService.behandleInntektsmelding(inntektsmelding);

        verify(oppgavebehandlingConsumer).opprettOppgave(anyString(), any(Oppgave.class));
        verify(oppgavebehandlingConsumer, never()).oppdaterOppgavebeskrivelse(any(Oppgave.class), anyString());
    }
}