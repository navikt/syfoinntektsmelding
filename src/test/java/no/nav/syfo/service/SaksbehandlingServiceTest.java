package no.nav.syfo.service;

import no.nav.syfo.consumer.rest.EksisterendeSakConsumer;
import no.nav.syfo.consumer.rest.aktor.AktorConsumer;
import no.nav.syfo.consumer.ws.BehandleSakConsumer;
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer;
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer;
import no.nav.syfo.domain.GeografiskTilknytningData;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.domain.Periode;
import no.nav.syfo.repository.InntektsmeldingDAO;
import no.nav.syfo.repository.InntektsmeldingMeta;
import no.nav.syfo.util.Metrikk;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
    private AktorConsumer aktoridConsumer;
    @Mock
    private InntektsmeldingDAO inntektsmeldingDAO;
    @Mock
    private EksisterendeSakConsumer eksisterendeSakConsumer;
    @Mock
    private Metrikk metrikk;

    @InjectMocks
    private SaksbehandlingService saksbehandlingService;

    @Before
    public void setup() {
        when(inntektsmeldingDAO.finnBehandledeInntektsmeldinger(any())).thenReturn(emptyList());
        when(aktoridConsumer.getAktorId(anyString())).thenReturn("aktorid");
        when(behandlendeEnhetConsumer.hentGeografiskTilknytning(anyString())).thenReturn(GeografiskTilknytningData.builder().geografiskTilknytning("Geografisktilknytning").build());
        when(behandlendeEnhetConsumer.hentBehandlendeEnhet(anyString())).thenReturn("behandlendeenhet1234");
        when(behandleSakConsumer.opprettSak("fnr")).thenReturn("opprettetSaksId");
        when(eksisterendeSakConsumer.finnEksisterendeSaksId(any(), anyString())).thenReturn(Optional.of("saksId"));
    }

    private Inntektsmelding lagInntektsmelding() {
        return Inntektsmelding.builder()
                .fnr("fnr")
                .arbeidsgiverOrgnummer("orgnummer")
                .journalpostId("journalpostId")
                .arbeidsforholdId(null)
                .arsakTilInnsending("Ny")
                .arbeidsgiverperioder(singletonList(Periode.builder()
                        .fom(LocalDate.of(2019, 1, 4))
                        .tom(LocalDate.of(2019, 1, 20))
                        .build()))
                .build();
    }

    @Test
    public void returnererSaksIdOmSakFinnes() {
        String saksId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");

        assertThat(saksId).isEqualTo("saksId");
    }

    @Test
    public void oppretterSakOmSakIkkeFinnes() {
        when(eksisterendeSakConsumer.finnEksisterendeSaksId(any(), anyString())).thenReturn(Optional.empty());

        String saksId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");

        assertThat(saksId).isEqualTo("opprettetSaksId");
    }

    @Test
    public void oppretterOppgaveForSak() {
        saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");

        verify(oppgavebehandlingConsumer).opprettOppgave(anyString(), any(Oppgave.class));
    }

    @Test(expected = RuntimeException.class)
    public void test() throws HentAktoerIdForIdentPersonIkkeFunnet {
        when(aktoridConsumer.getAktorId(anyString())).thenThrow(HentAktoerIdForIdentPersonIkkeFunnet.class);

        saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");
    }

    @Test
    public void girNyInntektsmeldingEksisterendeSakIdOmTomOverlapper() {
        when(inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId")).thenReturn(singletonList(
                InntektsmeldingMeta
                        .builder()
                        .sakId("1")
                        .arbeidsgiverperioder(asList(Periode.builder()
                                .fom(LocalDate.of(2019, 1, 1))
                                .tom(LocalDate.of(2019, 1, 20))
                                .build()))
                        .build()
        ));

        String sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");
        assertThat(sakId).isEqualTo("1");
        verify(behandleSakConsumer, never()).opprettSak(anyString());
    }

    @Test
    public void girNyInntektsmeldingEksisterendeSakIdOmFomOverlapper() {
        when(inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId")).thenReturn(singletonList(
                InntektsmeldingMeta
                        .builder()
                        .sakId("1")
                        .arbeidsgiverperioder(asList(Periode.builder()
                                .fom(LocalDate.of(2019, 1, 4))
                                .tom(LocalDate.of(2019, 1, 24))
                                .build()))
                        .build()
        ));

        String sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");
        assertThat(sakId).isEqualTo("1");
        verify(behandleSakConsumer, never()).opprettSak(anyString());
    }

    @Test
    public void girNyInntektsmeldingEksisterendeSakIdOmFomOgTomOverlapper() {
        when(inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId")).thenReturn(singletonList(
                InntektsmeldingMeta
                        .builder()
                        .sakId("1")
                        .arbeidsgiverperioder(asList(Periode.builder()
                                .fom(LocalDate.of(2019, 1, 4))
                                .tom(LocalDate.of(2019, 1, 20))
                                .build()))
                        .build()
        ));

        String sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");
        assertThat(sakId).isEqualTo("1");
        verify(behandleSakConsumer, never()).opprettSak(anyString());
    }
}
