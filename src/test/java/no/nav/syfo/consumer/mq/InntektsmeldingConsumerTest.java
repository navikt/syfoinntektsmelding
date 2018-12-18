package no.nav.syfo.consumer.mq;

import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.service.JournalpostService;
import no.nav.syfo.service.SaksbehandlingService;
import no.nav.syfo.util.Metrikk;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jms.MessageNotWriteableException;

import static no.nav.syfo.domain.InngaaendeJournal.MIDLERTIDIG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InntektsmeldingConsumerTest {

    @Mock
    private Metrikk metrikk;

    @Mock
    private JournalpostService journalpostService;

    @Mock
    private SaksbehandlingService saksbehandlingService;

    @InjectMocks
    private InntektsmeldingConsumer inntektsmeldingConsumer;

    @Test
    public void behandlerInntektsmelding() throws MessageNotWriteableException {
        when(journalpostService.hentInntektsmelding("arkivId")).thenReturn(Inntektsmelding.builder()
                .status(MIDLERTIDIG)
                .journalpostId("akrivId")
                .build());
        when(saksbehandlingService.behandleInntektsmelding(any())).thenReturn("saksId");

        ActiveMQTextMessage message = new ActiveMQTextMessage();
        message.setText("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "  <ns5:forsendelsesinformasjon xmlns:ns5=\"http://nav.no/melding/virksomhet/dokumentnotifikasjon/v1\" " +
                "    xmlns:ns2=\"http://nav.no/melding/virksomhet/dokumentforsendelse/v1\" " +
                "    xmlns:ns4=\"http://nav.no/dokmot/jms/reply\" " +
                "    xmlns:ns3=\"http://nav.no.dokmot/jms/viderebehandling\">" +
                "  <arkivId>arkivId</arkivId>" +
                "  <arkivsystem>JOARK</arkivsystem>" +
                "  <tema>SYK</tema>" +
                "  <behandlingstema>ab0061</behandlingstema>" +
                "</ns5:forsendelsesinformasjon>");
        inntektsmeldingConsumer.listen(message);

        verify(saksbehandlingService).behandleInntektsmelding(any());
        verify(journalpostService).ferdigstillJournalpost(any(), any());
    }

    @Test
    public void behandlerIkkeInntektsmeldingMedStatusForskjelligFraMidlertidig() throws MessageNotWriteableException {
        when(journalpostService.hentInntektsmelding("arkivId")).thenReturn(Inntektsmelding.builder()
                .status("ANNET")
                .journalpostId("arkivId")
                .build());

        ActiveMQTextMessage message = new ActiveMQTextMessage();
        message.setText("" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "  <ns5:forsendelsesinformasjon xmlns:ns5=\"http://nav.no/melding/virksomhet/dokumentnotifikasjon/v1\" " +
                "    xmlns:ns2=\"http://nav.no/melding/virksomhet/dokumentforsendelse/v1\" " +
                "    xmlns:ns4=\"http://nav.no/dokmot/jms/reply\" " +
                "    xmlns:ns3=\"http://nav.no.dokmot/jms/viderebehandling\">" +
                "  <arkivId>arkivId</arkivId>" +
                "  <arkivsystem>JOARK</arkivsystem>" +
                "  <tema>SYK</tema>" +
                "  <behandlingstema>ab0061</behandlingstema>" +
                "</ns5:forsendelsesinformasjon>");
        inntektsmeldingConsumer.listen(message);

        verify(saksbehandlingService, never()).behandleInntektsmelding(any());
        verify(journalpostService, never()).ferdigstillJournalpost(any(), any());
    }
}