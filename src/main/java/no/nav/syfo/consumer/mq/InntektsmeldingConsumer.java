package no.nav.syfo.consumer.mq;

import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon;
import no.nav.syfo.consumer.rest.AktorConsumer;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.repository.InntektsmeldingDAO;
import no.nav.syfo.repository.InntektsmeldingMeta;
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
    private InntektsmeldingDAO inntektsmeldingDAO;
    private AktorConsumer aktorConsumer;

    public InntektsmeldingConsumer(
            JournalpostService journalpostService,
            SaksbehandlingService saksbehandlingService,
            Metrikk metrikk, InntektsmeldingDAO inntektsmeldingDAO, AktorConsumer aktorConsumer) {
        this.journalpostService = journalpostService;
        this.saksbehandlingService = saksbehandlingService;
        this.metrikk = metrikk;
        this.inntektsmeldingDAO = inntektsmeldingDAO;
        this.aktorConsumer = aktorConsumer;
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

            String aktorid = aktorConsumer.getAktorId(inntektsmelding.getFnr());

            if (MIDLERTIDIG.equals(inntektsmelding.getStatus())) {
                metrikk.tellInntektsmeldingerMottatt(inntektsmelding);

                String saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding, aktorid);

                journalpostService.ferdigstillJournalpost(saksId, inntektsmelding);

                inntektsmeldingDAO.opprett(
                        InntektsmeldingMeta.builder()
                                .orgnummer(inntektsmelding.getArbeidsgiverOrgnummer())
                                .aktorId(aktorid)
                                .sakId(saksId)
                                .arbeidsgiverperiodeFom(inntektsmelding.getArbeidsgiverperiodeFom())
                                .arbeidsgiverperiodeTom(inntektsmelding.getArbeidsgiverperiodeTom())
                                .build());

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
