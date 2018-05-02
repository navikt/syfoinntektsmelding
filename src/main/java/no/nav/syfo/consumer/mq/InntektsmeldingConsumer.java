package no.nav.syfo.consumer.mq;

import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon;
import no.nav.syfo.consumer.ws.BehandleInngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.BehandleSakConsumer;
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.JournalConsumer;
import no.nav.syfo.domain.InngaendeJournalpost;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.domain.SyfoException;
import no.nav.syfo.service.JournalpostService;
import no.nav.syfo.service.PeriodeService;
import no.nav.syfo.service.SaksbehandlingService;
import no.nav.syfo.util.JAXB;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;
import java.time.LocalDate;

import static java.util.Optional.ofNullable;
import static no.nav.syfo.util.MDCOperations.*;

@Component
@Slf4j
public class InntektsmeldingConsumer {

    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    private JournalConsumer journalConsumer;
    private PeriodeService periodeService;
    private BehandleSakConsumer behandleSakConsumer;
    private SaksbehandlingService saksbehandlingService;
    private BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer;
    private JournalpostService journalpostService;

    public InntektsmeldingConsumer(InngaaendeJournalConsumer inngaaendeJournalConsumer, JournalConsumer journalConsumer, BehandleSakConsumer behandleSakConsumer, SaksbehandlingService saksbehandlingService, BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer, PeriodeService periodeService, JournalpostService journalpostService) {
        this.inngaaendeJournalConsumer = inngaaendeJournalConsumer;
        this.journalConsumer = journalConsumer;
        this.behandleSakConsumer = behandleSakConsumer;
        this.saksbehandlingService = saksbehandlingService;
        this.behandleInngaaendeJournalConsumer = behandleInngaaendeJournalConsumer;
        this.periodeService = periodeService;
        this.journalpostService = journalpostService;
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

            String journalpostId = info.getArkivId();
            String dokumentId = inngaaendeJournalConsumer.hentDokumentId(journalpostId);
            Inntektsmelding inntektsmelding = journalConsumer.hentInntektsmelding(journalpostId, dokumentId);

            if (!periodeService.erSendtInnSoknadForPeriode()) {
                String saksId = behandleSakConsumer.opprettSak(inntektsmelding.getFnr());
                saksbehandlingService.opprettOppgave(
                        inntektsmelding.getFnr(),
                        Oppgave.builder()
                                .journalpostId(journalpostId)
                                .gsakSaksid(saksId)
                                .beskrivelse("Det har kommet en inntektsmelding på sykepenger.")
                                // TODO: Hva skal aktivTil være?
                                .aktivTil(LocalDate.of(2018, 5, 1))
                                .build());

                InngaendeJournalpost journalpost = journalpostService.hentInngaendeJournalpost(journalpostId, saksId);

                behandleInngaaendeJournalConsumer.oppdaterJournalpost(journalpost);
                behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost);
            }

            log.info("behandlet melding om inntektskjema - arkivid: {}, arkivsystem: {}, tema: {}, behandingstema: {}",
                    info.getArkivId(),
                    info.getArkivsystem(),
                    info.getTema().getValue(),
                    info.getBehandlingstema().getValue());

        } catch (JMSException e) {
            log.error("Feil med parsing av inntektsmelding fra kø", e);
            throw new SyfoException("Feil ved lesing av melding", e);
        }
    }
}
