package no.nav.syfo.consumer.mq;

import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.service.JournalpostService;
import no.nav.syfo.service.SaksbehandlingService;
import no.nav.syfo.util.JAXB;
import no.nav.syfo.util.Metrikk;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;

import static java.util.Optional.ofNullable;
import static no.nav.syfo.domain.InngaaendeJournal.MIDLERTIDIG;
import static no.nav.syfo.util.MDCOperations.*;

@Component
@Slf4j
public class InntektsmeldingConsumer {

    private JournalpostService journalpostService;
    private SaksbehandlingService saksbehandlingService;
    private Metrikk metrikk;

    public InntektsmeldingConsumer(
            JournalpostService journalpostService,
            SaksbehandlingService saksbehandlingService,
            Metrikk metrikk) {
        this.journalpostService = journalpostService;
        this.saksbehandlingService = saksbehandlingService;
        this.metrikk = metrikk;
    }

    @Transactional
    @JmsListener(id = "inntektsmelding_listener", containerFactory = "jmsListenerContainerFactory", destination = "inntektsmeldingQueue")
    public void listen(Object message) {
        try {
            TextMessage textMessage = (TextMessage) message;
            putToMDC(MDC_CALL_ID, ofNullable(textMessage.getStringProperty("callId")).orElse(generateCallId()));
            JAXBElement<XMLForsendelsesinformasjon> xmlForsendelsesinformasjon = JAXB.unmarshalForsendelsesinformasjon(textMessage.getText());
            final XMLForsendelsesinformasjon info = xmlForsendelsesinformasjon.getValue();

            Inntektsmelding inntektsmelding = journalpostService.hentInntektsmelding(info.getArkivId());

            if (MIDLERTIDIG.equals(inntektsmelding.getStatus())) {
                metrikk.tellInntektsmeldingerMottat(inntektsmelding);

                String saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding);

                journalpostService.ferdigstillJournalpost(saksId, inntektsmelding);

            // Lagre det vi trenger fra inntektsmeldingen

                log.info("Inntektsmelding {} er journalført", inntektsmelding.getJournalpostId());
            } else {
                log.info("Behandler ikke inntektsmelding {} da den har status: {}", inntektsmelding.getJournalpostId(), inntektsmelding.getStatus());
            }
        } catch (JMSException e) {
            log.error("Feil ved parsing av inntektsmelding fra kø", e);
            metrikk.tellInntektsmeldingfeil();
            throw new RuntimeException("Feil ved lesing av melding", e);
        } catch (Exception e) {
            log.error("Det skjedde en feil ved journalføring", e);
            metrikk.tellInntektsmeldingfeil();
            throw new RuntimeException("Det skjedde en feil ved journalføring", e);
        } finally {
            remove(MDC_CALL_ID);
        }
    }
}
