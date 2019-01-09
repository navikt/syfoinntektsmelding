package no.nav.syfo.consumer.mq;

import no.nav.syfo.LocalApplication;
import no.nav.syfo.consumer.rest.AktorConsumer;
import no.nav.syfo.consumer.rest.EksisterendeSakConsumer;
import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.GeografiskTilknytningData;
import no.nav.syfo.domain.InngaaendeJournal;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.repository.InntektsmeldingDAO;
import no.nav.syfo.repository.InntektsmeldingMeta;
import no.nav.syfo.util.Metrikk;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import javax.jms.MessageNotWriteableException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.syfo.domain.InngaaendeJournal.MIDLERTIDIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalApplication.class)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext
public class InntektsmeldingConsumerIntegrassjonsTest {

    @MockBean
    private InngaaendeJournalConsumer inngaaendeJournalConsumer;

    @MockBean
    private JournalConsumer journalConsumer;

    @MockBean
    private AktorConsumer aktorConsumer;

    @MockBean
    private EksisterendeSakConsumer eksisterendeSakConsumer;

    @MockBean
    private BehandleSakConsumer behandleSakConsumer;

    @MockBean
    private BehandlendeEnhetConsumer behandlendeEnhetConsumer;

    @MockBean
    private OppgavebehandlingConsumer oppgavebehandlingConsumer;

    @MockBean
    private BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer;

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
        jdbcTemplate.update("DELETE FROM INNTEKTSMELDING");
        saksIdteller = 0;

        InngaaendeJournal inngaaendeJournal = inngaaendeJournal("arkivId");

        when(behandleSakConsumer.opprettSak("fnr")).thenAnswer(invocation -> "saksId"+saksIdteller++);

        when(inngaaendeJournalConsumer.hentDokumentId("arkivId")).thenReturn(inngaaendeJournal);

        when(journalConsumer.hentInntektsmelding("arkivId", inngaaendeJournal)).thenReturn(
                inntektsmelding("arkivId", LocalDate.of(2019,1,1), LocalDate.of(2019,1,16)));

        when(aktorConsumer.getAktorId("fnr")).thenReturn("aktorId");
        when(eksisterendeSakConsumer.finnEksisterendeSaksId("aktorId", "orgnummer")).thenReturn(Optional.empty());

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
    public void test() throws MessageNotWriteableException {
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId"));
    }

    @Test
    public void gjenbrukerSaksIdHvisViFarToOverlappendeInntektsmeldinger() throws MessageNotWriteableException {
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"));
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"));

        when(journalConsumer.hentInntektsmelding("arkivId1", inngaaendeJournal("arkivId1"))).thenReturn(inntektsmelding("arkivId1", LocalDate.of(2019, 1,1), LocalDate.of(2019,1,16)));
        when(journalConsumer.hentInntektsmelding("arkivId2", inngaaendeJournal("arkivId2"))).thenReturn(inntektsmelding("arkivId2", LocalDate.of(2019, 1,2), LocalDate.of(2019,1,16)));

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId1"));
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"));

        List<InntektsmeldingMeta> inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId", "orgnummer");
        assertThat(inntektsmeldingMetas.size()).isEqualTo(2);
        assertThat(inntektsmeldingMetas.get(0).getSakId()).isEqualTo(inntektsmeldingMetas.get(1).getSakId());

        verify(behandleSakConsumer, times(1)).opprettSak("fnr");
    }

    @Test
    public void gjenbrukerIkkeSaksIdHvisViFarToInntektsmeldingerSomIkkeOverlapper() throws MessageNotWriteableException {
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"));
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"));

        when(journalConsumer.hentInntektsmelding("arkivId1", inngaaendeJournal("arkivId1"))).thenReturn(inntektsmelding("arkivId1", LocalDate.of(2019, 1,1), LocalDate.of(2019,1,16)));
        when(journalConsumer.hentInntektsmelding("arkivId2", inngaaendeJournal("arkivId2"))).thenReturn(inntektsmelding("arkivId2", LocalDate.of(2019, 2,1), LocalDate.of(2019,2,16)));

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId1"));
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"));

        List<InntektsmeldingMeta> inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId", "orgnummer");
        assertThat(inntektsmeldingMetas.size()).isEqualTo(2);
        assertThat(inntektsmeldingMetas.get(0).getSakId()).isNotEqualTo(inntektsmeldingMetas.get(1).getSakId());

        assertThat(inntektsmeldingMetas.get(0).getSakId()).isEqualTo("saksId0");
        assertThat(inntektsmeldingMetas.get(1).getSakId()).isEqualTo("saksId1");

        verify(behandleSakConsumer, times(2)).opprettSak("fnr");
    }

    @Test
    public void brukerSaksIdFraSykeforloepOmViIkkeHarOverlappendeInntektsmelding() throws MessageNotWriteableException {
        when(eksisterendeSakConsumer.finnEksisterendeSaksId("aktorId", "orgnummer")).thenReturn(Optional.empty(), Optional.of("syfosak"));
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId1")).thenReturn(inngaaendeJournal("arkivId1"));
        when(inngaaendeJournalConsumer.hentDokumentId("arkivId2")).thenReturn(inngaaendeJournal("arkivId2"));

        when(journalConsumer.hentInntektsmelding("arkivId1", inngaaendeJournal("arkivId1"))).thenReturn(inntektsmelding("arkivId1", LocalDate.of(2019, 1,1), LocalDate.of(2019,1,16)));
        when(journalConsumer.hentInntektsmelding("arkivId2", inngaaendeJournal("arkivId2"))).thenReturn(inntektsmelding("arkivId2", LocalDate.of(2019, 2,1), LocalDate.of(2019,2,16)));

        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId1"));
        inntektsmeldingConsumer.listen(opprettKoemelding("arkivId2"));

        List<InntektsmeldingMeta> inntektsmeldingMetas = inntektsmeldingDAO.finnBehandledeInntektsmeldinger("aktorId", "orgnummer");
        assertThat(inntektsmeldingMetas.size()).isEqualTo(2);
        assertThat(inntektsmeldingMetas.get(0).getSakId()).isNotEqualTo(inntektsmeldingMetas.get(1).getSakId());

        assertThat(inntektsmeldingMetas.get(0).getSakId()).isEqualTo("saksId0");
        assertThat(inntektsmeldingMetas.get(1).getSakId()).isEqualTo("syfosak");

        verify(behandleSakConsumer, times(1)).opprettSak("fnr");
    }

    private Inntektsmelding inntektsmelding(String arkivId, LocalDate fom, LocalDate tom) {
        return Inntektsmelding
                .builder()
                .fnr("fnr")
                .journalpostId(arkivId)
                .arbeidsforholdId("arbeidsforholdId")
                .arbeidsgiverOrgnummer("orgnummer")
                .arsakTilInnsending("arsak")
                .arbeidsgiverperiodeFom(fom)
                .arbeidsgiverperiodeTom(tom)
                .status(MIDLERTIDIG)
                .build();
    }

    private ActiveMQTextMessage opprettKoemelding(String arkivId) throws MessageNotWriteableException {
        ActiveMQTextMessage message = new ActiveMQTextMessage();
        message.setText("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "  <ns5:forsendelsesinformasjon xmlns:ns5=\"http://nav.no/melding/virksomhet/dokumentnotifikasjon/v1\" " +
                "    xmlns:ns2=\"http://nav.no/melding/virksomhet/dokumentforsendelse/v1\" " +
                "    xmlns:ns4=\"http://nav.no/dokmot/jms/reply\" " +
                "    xmlns:ns3=\"http://nav.no.dokmot/jms/viderebehandling\">" +
                "  <arkivId>"+arkivId+"</arkivId>" +
                "  <arkivsystem>JOARK</arkivsystem>" +
                "  <tema>SYK</tema>" +
                "  <behandlingstema>ab0061</behandlingstema>" +
                "</ns5:forsendelsesinformasjon>");

        return message;
    }
}
