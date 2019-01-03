package no.nav.syfo.service;

import no.nav.syfo.consumer.rest.AktorConsumer;
import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.*;
import no.nav.syfo.repository.InntektsmeldingDAO;
import no.nav.syfo.repository.InntektsmeldingMeta;
import no.nav.syfo.repository.SykepengesoknadDAO;
import no.nav.syfo.repository.SykmeldingDAO;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

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
    private SykepengesoknadDAO sykepengesoknadDAO;
    @Mock
    private AktorConsumer aktoridConsumer;
    @Mock
    private SykmeldingDAO sykmeldingDAO;
    @Mock
    private InntektsmeldingDAO inntektsmeldingDAO;

    @InjectMocks
    private SaksbehandlingService saksbehandlingService;

    @Before
    public void setup() {
        when(inntektsmeldingDAO.finnBehandledeInntektsmeldinger(any(), anyString())).thenReturn(emptyList());
        when(aktoridConsumer.getAktorId(anyString())).thenReturn("aktorid");
        when(sykmeldingDAO.hentSykmeldingerForOrgnummer(anyString(), anyString()))
                .thenReturn(singletonList(Sykmelding.builder().orgnummer("orgnummer").id(123).build()));
        when(behandlendeEnhetConsumer.hentGeografiskTilknytning(anyString())).thenReturn(GeografiskTilknytningData.builder().geografiskTilknytning("Geografisktilknytning").build());
        when(behandlendeEnhetConsumer.hentBehandlendeEnhet(anyString())).thenReturn("behandlendeenhet1234");
        when(behandleSakConsumer.opprettSak("fnr")).thenReturn("opprettetSaksId");
        when(sykepengesoknadDAO.hentSykepengesoknaderForPerson(anyString(), anyInt()))
                .thenReturn(new ArrayList<Sykepengesoknad>() {{
                                add(Sykepengesoknad.builder()
                                        .fom(LocalDate.of(2018, 1, 1))
                                        .tom(LocalDate.of(2018, 2, 1))
                                        .saksId("saksId")
                                        .oppgaveId("oppgaveId")
                                        .build());
                            }}
                );
    }

    private Inntektsmelding lagInntektsmelding() {
        return Inntektsmelding.builder()
                .fnr("fnr")
                .arbeidsgiverOrgnummer("orgnummer")
                .journalpostId("journalpostId")
                .arbeidsforholdId(null)
                .arsakTilInnsending("Ny")
                .arbeidsgiverperiodeFom(LocalDate.of(2019,1,4))
                .arbeidsgiverperiodeTom(LocalDate.of(2019,1,20))
                .build();
    }

    @Test
    public void returnererSaksIdOmSakFinnes() {
        String saksId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");

        assertThat(saksId).isEqualTo("saksId");
    }

    @Test
    public void oppretterSakOmSakIkkeFinnes() {
        when(sykmeldingDAO.hentSykmeldingerForOrgnummer(anyString(), anyString())).thenReturn(emptyList());

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
    public void girNyInntektsmeldingEksisterendeSakIdOmTomOverlapper () {
        when(inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId", "orgnummer")).thenReturn(singletonList(
                InntektsmeldingMeta
                        .builder()
                        .sakId("1")
                        .arbeidsgiverperiodeFom(LocalDate.of(2019, 1, 1))
                        .arbeidsgiverperiodeTom(LocalDate.of(2019, 1, 20))
                        .build()
        ));

        String sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");
        assertThat(sakId).isEqualTo("1");
        verify(behandleSakConsumer, never()).opprettSak(anyString());
    }

    @Test
    public void girNyInntektsmeldingEksisterendeSakIdOmFomOverlapper () {
        when(inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId", "orgnummer")).thenReturn(singletonList(
                InntektsmeldingMeta
                        .builder()
                        .sakId("1")
                        .arbeidsgiverperiodeFom(LocalDate.of(2019, 1, 4))
                        .arbeidsgiverperiodeTom(LocalDate.of(2019, 1, 24))
                        .build()
        ));

        String sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");
        assertThat(sakId).isEqualTo("1");
        verify(behandleSakConsumer, never()).opprettSak(anyString());
    }

    @Test
    public void girNyInntektsmeldingEksisterendeSakIdOmFomOgTomOverlapper () {
        when(inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId", "orgnummer")).thenReturn(singletonList(
                InntektsmeldingMeta
                        .builder()
                        .sakId("1")
                        .arbeidsgiverperiodeFom(LocalDate.of(2019, 1, 4))
                        .arbeidsgiverperiodeTom(LocalDate.of(2019, 1, 20))
                        .build()
        ));

        String sakId = saksbehandlingService.behandleInntektsmelding(lagInntektsmelding(), "aktorId");
        assertThat(sakId).isEqualTo("1");
        verify(behandleSakConsumer, never()).opprettSak(anyString());
    }
}
