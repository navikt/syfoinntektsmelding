package no.nav.syfo.consumer.mq;

import no.nav.syfo.LocalApplication;
import no.nav.syfo.consumer.rest.aktor.AktorConsumer;
import no.nav.syfo.service.EksisterendeSakService;
import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.GeografiskTilknytningData;
import no.nav.syfo.domain.InngaaendeJournal;
import no.nav.syfo.domain.Periode;
import no.nav.syfo.repository.InntektsmeldingDAO;
import no.nav.syfo.repository.InntektsmeldingMeta;
import no.nav.syfo.util.Metrikk;
import no.nav.tjeneste.virksomhet.behandle.inngaaende.journal.v1.BehandleInngaaendeJournalV1;
import no.nav.tjeneste.virksomhet.journal.v2.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v2.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v2.JournalV2;
import no.nav.tjeneste.virksomhet.journal.v2.WSHentDokumentResponse;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import javax.jms.MessageNotWriteableException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.syfo.domain.InngaaendeJournal.MIDLERTIDIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
public class InntektsmeldingConsumerIntegrassjonsTest {

    @MockBean
    private InngaaendeJournalConsumer inngaaendeJournalConsumer;

    @MockBean
    private JournalV2 journalV2;

    @MockBean
    private AktorConsumer aktorConsumer;

    @MockBean
    private EksisterendeSakService eksisterendeSakService;

    @MockBean
    private BehandleSakConsumer behandleSakConsumer;

    @MockBean
    private BehandlendeEnhetConsumer behandlendeEnhetConsumer;

    @MockBean
    private OppgavebehandlingConsumer oppgavebehandlingConsumer;

    @MockBean
    private BehandleInngaaendeJournalV1 behandleInngaaendeJournalV1;

    @MockBean
    private Metrikk metrikk;

    @Inject
    private InntektsmeldingConsumer inntektsmeldingConsumer;

    @Inject
    private InntektsmeldingDAO inntektsmeldingDAO;

    @Inject
    private JdbcTemplate jdbcTemplate;

    private int saksIdteller = 0;

    @Before
    public void setup() {
        jdbcTemplate.update("DELETE FROM ARBEIDSGIVERPERIODE");
        jdbcTemplate.update("DELETE FROM INNTEKTSMELDING");
        saksIdteller = 0;

        InngaaendeJournal inngaaendeJournal = inngaaendeJournal("arkivId");

        when(behandleSakConsumer.opprettSak("fnr")).thenAnswer(invocation -> "saksId" + saksIdteller++);

        when(inngaaendeJournalConsumer.hentDokumentId("arkivId")).thenReturn(inngaaendeJournal);

        when(aktorConsumer.getAktorId("fnr")).thenReturn("aktorId");
        when(eksisterendeSakService.finnEksisterendeSak("aktorId", "orgnummer")).thenReturn(null);

        when(behandlendeEnhetConsumer.hentBehandlendeEnhet("fnr")).thenReturn("enhet");
        when(behandlendeEnhetConsumer.hentGeografiskTilknytning("fnr")).thenReturn(GeografiskTilknytningData.builder().geografiskTilknytning("tilknytning").diskresjonskode("").build());
    }

    private InngaaendeJournal inngaaendeJournal(String arkivId) {
        return InngaaendeJournal
                .builder()
                .status(MIDLERTIDIG)
                .dokumentId(arkivId)
                .build();
    }

    @Test
    public void gjenbrukerSaksIdHvisViFarToOverlappendeInntektsmeldinger() throws MessageNotWriteableException, HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"));
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"));

        when(journalV2.hentDokument(any())).thenReturn(
                new WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiver(
                        Collections.singletonList(Periode.builder().fom(LocalDate.of(2019, 1, 1)).tom(LocalDate.of(2019, 1, 16)).build())
                ).getBytes()),
                new WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiver(
                        Collections.singletonList(Periode.builder().fom(LocalDate.of(2019, 1, 2)).tom(LocalDate.of(2019, 1, 16)).build())
                ).getBytes())
        );

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId1"));
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"));

        List<InntektsmeldingMeta> inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId");
        assertThat(inntektsmeldingMetas.size()).isEqualTo(2);
        assertThat(inntektsmeldingMetas.get(0).getSakId()).isEqualTo(inntektsmeldingMetas.get(1).getSakId());

        verify(behandleSakConsumer, times(1)).opprettSak("fnr");
    }

    @Test
    public void gjenbrukerIkkeSaksIdHvisViFarToInntektsmeldingerSomIkkeOverlapper() throws MessageNotWriteableException, HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"));
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"));

        when(journalV2.hentDokument(any())).thenReturn(
                new WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiver(
                        Collections.singletonList(Periode.builder().fom(LocalDate.of(2019, 1, 1)).tom(LocalDate.of(2019, 1, 16)).build())
                ).getBytes()),
                new WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiver(
                        Collections.singletonList(Periode.builder().fom(LocalDate.of(2019, 2, 2)).tom(LocalDate.of(2019, 2, 16)).build())
                ).getBytes())
        );

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId1"));
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"));

        List<InntektsmeldingMeta> inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId");
        assertThat(inntektsmeldingMetas.size()).isEqualTo(2);
        assertThat(inntektsmeldingMetas.get(0).getSakId()).isNotEqualTo(inntektsmeldingMetas.get(1).getSakId());

        assertThat(inntektsmeldingMetas.get(0).getSakId()).isEqualTo("saksId0");
        assertThat(inntektsmeldingMetas.get(1).getSakId()).isEqualTo("saksId1");

        verify(behandleSakConsumer, times(2)).opprettSak("fnr");
    }

    @Test
    public void brukerSaksIdFraSykeforloepOmViIkkeHarOverlappendeInntektsmelding() throws MessageNotWriteableException, HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(eksisterendeSakService.finnEksisterendeSak("aktorId", "orgnummer")).thenReturn(null, "syfosak");
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"));
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"));

        when(journalV2.hentDokument(any())).thenReturn(
                new WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiver(
                        Collections.singletonList(Periode.builder().fom(LocalDate.of(2019, 1, 1)).tom(LocalDate.of(2019, 1, 16)).build())
                ).getBytes()),
                new WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiver(
                        Collections.singletonList(Periode.builder().fom(LocalDate.of(2019, 2, 2)).tom(LocalDate.of(2019, 2, 16)).build())
                ).getBytes())
        );

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId1"));
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"));

        List<InntektsmeldingMeta> inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId");
        assertThat(inntektsmeldingMetas.size()).isEqualTo(2);
        assertThat(inntektsmeldingMetas.get(0).getSakId()).isNotEqualTo(inntektsmeldingMetas.get(1).getSakId());

        assertThat(inntektsmeldingMetas.get(0).getSakId()).isEqualTo("saksId0");
        assertThat(inntektsmeldingMetas.get(1).getSakId()).isEqualTo("syfosak");

        verify(behandleSakConsumer, times(1)).opprettSak("fnr");
    }

    @Test
    public void mottarInntektsmeldingUtenArbeidsgiverperioder() throws MessageNotWriteableException, HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(journalV2.hentDokument(any())).thenReturn(
                new WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiver(emptyList()).getBytes())
        );

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId"));

        List<InntektsmeldingMeta> inntektsmeldinger = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId");

        assertThat(inntektsmeldinger.get(0).getArbeidsgiverperioder().size()).isEqualTo(0);
    }

    @Test
    public void mottarInntektsmeldingMedFlerePerioder() throws MessageNotWriteableException, HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(journalV2.hentDokument(any())).thenReturn(
                new WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiver(
                        asList(Periode.builder().fom(LocalDate.of(2019, 1, 1)).tom(LocalDate.of(2019, 1, 12)).build(),
                                Periode.builder().fom(LocalDate.of(2019, 1, 12)).tom(LocalDate.of(2019, 1, 14)).build())
                ).getBytes())
        );

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId"));

        List<InntektsmeldingMeta> inntektsmeldinger = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId");

        assertThat(inntektsmeldinger.get(0).getArbeidsgiverperioder().size()).isEqualTo(2);
        assertThat(inntektsmeldinger.get(0).getArbeidsgiverperioder().get(0).getFom()).isEqualTo(LocalDate.of(2019, 1, 1));
        assertThat(inntektsmeldinger.get(0).getArbeidsgiverperioder().get(0).getTom()).isEqualTo(LocalDate.of(2019, 1, 12));
        assertThat(inntektsmeldinger.get(0).getArbeidsgiverperioder().get(1).getFom()).isEqualTo(LocalDate.of(2019, 1, 12));
        assertThat(inntektsmeldinger.get(0).getArbeidsgiverperioder().get(1).getTom()).isEqualTo(LocalDate.of(2019, 1, 14));
    }

    @Test
    public void mottarInntektsmeldingMedPrivatArbeidsgiver() throws MessageNotWriteableException, HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(journalV2.hentDokument(any())).thenReturn(
                new WSHentDokumentResponse().withDokument(JournalConsumerTest.inntektsmeldingArbeidsgiverPrivat().getBytes())
        );

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId"));

        List<InntektsmeldingMeta> inntektsmeldinger = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId");

        assertThat(inntektsmeldinger.get(0).getArbeidsgiverPrivat()).isEqualTo("arbeidsgiverPrivat");
        assertThat(inntektsmeldinger.get(0).getOrgnummer()).isNull();
        assertThat(inntektsmeldinger.get(0).getAktorId()).isEqualTo("aktorId");
    }

    private ActiveMQTextMessage opprettKoemelding(String arkivId) throws MessageNotWriteableException {
        ActiveMQTextMessage message = new ActiveMQTextMessage();
        message.setText("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "  <ns5:forsendelsesinformasjon xmlns:ns5=\"http://nav.no/melding/virksomhet/dokumentnotifikasjon/v1\" " +
                "    xmlns:ns2=\"http://nav.no/melding/virksomhet/dokumentforsendelse/v1\" " +
                "    xmlns:ns4=\"http://nav.no/dokmot/jms/reply\" " +
                "    xmlns:ns3=\"http://nav.no.dokmot/jms/viderebehandling\">" +
                "  <arkivId>" + arkivId + "</arkivId>" +
                "  <arkivsystem>JOARK</arkivsystem>" +
                "  <tema>SYK</tema>" +
                "  <behandlingstema>ab0061</behandlingstema>" +
                "</ns5:forsendelsesinformasjon>");

        return message;
    }
}
