package no.nav.syfo.consumer.ws;

import no.nav.syfo.service.SaksbehandlingService;
import no.nav.tjeneste.virksomhet.behandlesak.v1.BehandleSakV1;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.WSPerson;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.WSSak;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.WSOpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.WSOpprettSakResponse;
import no.nav.tjeneste.virksomhet.oppgave.v3.OppgaveV3;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.WSOppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.WSHentOppgaveRequest;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.WSHentOppgaveResponse;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.OppgavebehandlingV3;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSLagreOppgaveRequest;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgave;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgaveRequest;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgaveResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SaksbehandlingServiceTest {

    @Mock
    private BehandleSakV1 behandleSakV1;
    @Mock
    private OppgaveV3 oppgaveV3;
    @Mock
    private OppgavebehandlingV3 oppgavebehandlingV3;
    @Mock
    private PersonConsumer personConsumer;
    @Mock
    private ArbeidsfordelingConsumer arbeidsfordelingConsumer;

    @InjectMocks
    private SaksbehandlingService saksbehandlingService;

    @Test
    public void opprettSak() throws Exception {
        when(behandleSakV1.opprettSak(any())).thenReturn(new WSOpprettSakResponse().withSakId("1"));
        ArgumentCaptor<WSOpprettSakRequest> captor = ArgumentCaptor.forClass(WSOpprettSakRequest.class);

        String sakId = saksbehandlingService.opprettSak("12345678910");

        verify(behandleSakV1).opprettSak(captor.capture());
        WSSak sak = captor.getValue().getSak();

        assertThat(sakId).isEqualTo("1");
        assertThat(sak.getFagomraade().getValue()).isEqualTo("SYK");
        assertThat(sak.getFagsystem().getValue()).isEqualTo("FS22");
        assertThat(sak.getSakstype().getValue()).isEqualTo("GEN");
        assertThat(sak.getGjelderBrukerListe()).contains(new WSPerson().withIdent("12345678910"));
    }

    @Test
    public void finnOppgave() throws Exception {
        when(oppgaveV3.hentOppgave(any())).thenReturn(new WSHentOppgaveResponse().withOppgave(new WSOppgave().withOppgaveId("1234")));
        ArgumentCaptor<WSHentOppgaveRequest> captor = ArgumentCaptor.forClass(WSHentOppgaveRequest.class);

        WSOppgave oppgave = saksbehandlingService.finnOppgave("1234");

        verify(oppgaveV3).hentOppgave(captor.capture());

        assertThat(oppgave.getOppgaveId()).isEqualTo("1234");
        assertThat(captor.getValue().getOppgaveId()).isEqualTo("1234");
    }

    @Test
    public void opprettOppgave() throws Exception {
        when(oppgavebehandlingV3.opprettOppgave(any())).thenReturn(new WSOpprettOppgaveResponse().withOppgaveId("1234"));
        when(personConsumer.hentGeografiskTilknytning(anyString())).thenReturn("Geografisktilknytning");
        when(arbeidsfordelingConsumer.finnBehandlendeEnhet(anyString())).thenReturn("behandlendeenhet1234");
        ArgumentCaptor<WSOpprettOppgaveRequest> captor = ArgumentCaptor.forClass(WSOpprettOppgaveRequest.class);

        LocalDate aktivTil = LocalDate.of(2018, 1, 1);
        String beskrivelse = "Beskriv beskriv";
        String oppgaveId = saksbehandlingService.opprettOppgave("12345678910", beskrivelse, "gsak1234", "journalpost1234", aktivTil);

        verify(oppgavebehandlingV3).opprettOppgave(captor.capture());
        WSOpprettOppgave opprettOppgave = captor.getValue().getOpprettOppgave();

        assertThat(oppgaveId).isEqualTo("1234");
        assertThat(opprettOppgave.getBrukertypeKode()).isEqualTo("PERSON");
        assertThat(opprettOppgave.getOppgavetypeKode()).isEqualTo("INNT_SYK");
        assertThat(opprettOppgave.getFagomradeKode()).isEqualTo("SYK");
        assertThat(opprettOppgave.getUnderkategoriKode()).isEqualTo("SYK_SYK");
        assertThat(opprettOppgave.getPrioritetKode()).isEqualTo("NORM_SYK");
        assertThat(opprettOppgave.getBeskrivelse()).isEqualTo(beskrivelse);
        assertThat(opprettOppgave.getAktivTil()).isEqualTo(aktivTil);
        assertThat(opprettOppgave.getAnsvarligEnhetId()).isEqualTo("behandlendeenhet1234");
        assertThat(opprettOppgave.getDokumentId()).isEqualTo("journalpost1234");
        assertThat(opprettOppgave.getSaksnummer()).isEqualTo("gsak1234");
        assertThat(opprettOppgave.getOppfolging()).isEqualTo("\nDu kan gi oss tilbakemelding på søknaden om sykepenger.\n" +
                "Gå til internettadresse: nav.no/digitalsykmelding/tilbakemelding");
    }

    @Test
    public void oppdaterOppgavebeskrivelse() throws Exception {
        ArgumentCaptor<WSLagreOppgaveRequest> captor = ArgumentCaptor.forClass(WSLagreOppgaveRequest.class);

        String beskrivelse = "Beskriv beskriv";
        saksbehandlingService.oppdaterOppgavebeskrivelse(new WSOppgave().withOppgaveId("1234"), beskrivelse);

        verify(oppgavebehandlingV3).lagreOppgave(captor.capture());

        assertThat(captor.getValue().getEndreOppgave().getBeskrivelse()).isEqualTo(beskrivelse);
        assertThat(captor.getValue().getEndretAvEnhetId()).isEqualTo(9999);
    }

}