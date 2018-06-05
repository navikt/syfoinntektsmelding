package no.nav.syfo.consumer.mq;

import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.service.JournalpostService;
import no.nav.syfo.service.SaksbehandlingService;
import no.nav.syfo.util.JAXB;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;

import static java.util.Optional.ofNullable;
import static no.nav.syfo.util.MDCOperations.*;

@Component
@Slf4j
public class InntektsmeldingConsumer {

    private JournalpostService journalpostService;
    private SaksbehandlingService saksbehandlingService;

    public InntektsmeldingConsumer(JournalpostService journalpostService, SaksbehandlingService saksbehandlingService) {
        this.journalpostService = journalpostService;
        this.saksbehandlingService = saksbehandlingService;
    }

    @Transactional
    @JmsListener(id = "inntektsmelding_listener", containerFactory = "jmsListenerContainerFactory", destination = "inntektsmeldingQueue")
    public void listen(Object message) {

        TextMessage textMessage = (TextMessage) message;
        try {
            putToMDC(MDC_CALL_ID, ofNullable(textMessage.getStringProperty("callId")).orElse(generateCallId()));
            JAXBElement<XMLForsendelsesinformasjon> xmlForsendelsesinformasjon = JAXB.unmarshalForsendelsesinformasjon(textMessage.getText());
            final XMLForsendelsesinformasjon info = xmlForsendelsesinformasjon.getValue();
            log.info("Fikk melding om inntektskjema - arkivid: {}, arkivsystem: {}, tema: {}, behandingstema: {}",
                    info.getArkivId(),
                    info.getArkivsystem(),
                    info.getTema().getValue(),
                    info.getBehandlingstema().getValue());

            Inntektsmelding inntektsmelding = journalpostService.hentInntektsmelding(info.getArkivId());

            String saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding);

            journalpostService.ferdigstillJournalpost(saksId, inntektsmelding);

            log.info("Behandlet melding om inntektskjema - journalpost: {}", info.getArkivId());
        } catch (JMSException e) {
            log.error("Feil med parsing av inntektsmelding fra kø", e);
            throw new RuntimeException("Feil ved lesing av melding", e);
        }
    }
}
