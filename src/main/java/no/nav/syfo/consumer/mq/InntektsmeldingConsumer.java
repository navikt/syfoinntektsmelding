package no.nav.syfo.consumer.mq;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.exception.MeldingInboundException;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.TextMessage;

import static java.util.Optional.ofNullable;
import static no.nav.syfo.util.MDCOperations.*;

@Component
@Slf4j
public class InntektsmeldingConsumer {

    @Transactional
    @JmsListener(id = "inntektsmelding_listener", containerFactory = "jmsListenerContainerFactory", destination = "inntektsmeldingQueue")
    public void listen(Object message) {
        putToMDC(MDC_CALL_ID, generateCallId());
        TextMessage textMessage = (TextMessage) message;
        try {
            log.info("Fikk en melding: {}", textMessage.getText());
        } catch (JMSException e) {
            log.error("Feil ved lesing av melding", e);
            throw new MeldingInboundException("Feil ved lesing av melding", e);
        }
    }
}
