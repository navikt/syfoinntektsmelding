package no.nav.syfo.consumer.mq;

import lombok.extern.slf4j.Slf4j;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon;
import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.InngaendeJournalpost;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.repository.SykepengesoknadDAO;
import no.nav.syfo.service.JournalpostService;
import no.nav.syfo.service.SaksbehandlingService;
import no.nav.syfo.util.JAXB;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;
import java.time.LocalDate;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.syfo.util.MDCOperations.*;

@Component
@Slf4j
public class InntektsmeldingConsumer {

    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    private JournalConsumer journalConsumer;
    private BehandleSakConsumer behandleSakConsumer;
    private SaksbehandlingService saksbehandlingService;
    private BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer;
    private JournalpostService journalpostService;
    private SykepengesoknadDAO sykepengesoknadDAO;
    private OppgaveConsumer oppgaveConsumer;
    private OppgavebehandlingConsumer oppgavebehandlingConsumer;

    public InntektsmeldingConsumer(InngaaendeJournalConsumer inngaaendeJournalConsumer, JournalConsumer journalConsumer, BehandleSakConsumer behandleSakConsumer, SaksbehandlingService saksbehandlingService, BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer, JournalpostService journalpostService, SykepengesoknadDAO sykepengesoknadDAO, OppgaveConsumer oppgaveConsumer, OppgavebehandlingConsumer oppgavebehandlingConsumer) {
        this.inngaaendeJournalConsumer = inngaaendeJournalConsumer;
        this.journalConsumer = journalConsumer;
        this.behandleSakConsumer = behandleSakConsumer;
        this.saksbehandlingService = saksbehandlingService;
        this.behandleInngaaendeJournalConsumer = behandleInngaaendeJournalConsumer;
        this.journalpostService = journalpostService;
        this.sykepengesoknadDAO = sykepengesoknadDAO;
        this.oppgaveConsumer = oppgaveConsumer;
        this.oppgavebehandlingConsumer = oppgavebehandlingConsumer;
    }

    @Transactional
    // @JmsListener(id = "inntektsmelding_listener", containerFactory = "jmsListenerContainerFactory", destination = "inntektsmeldingQueue")
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

            Optional<Sykepengesoknad> sykepengesoknad = sykepengesoknadDAO.finnSykepengesoknad(journalpostId);

            Optional<Oppgave> oppgave = sykepengesoknad.isPresent()
                    ? oppgaveConsumer.finnOppgave(sykepengesoknad.get().getOppgaveId())
                    : Optional.empty();

            String saksId = sykepengesoknad.isPresent() && sykepengesoknad.get().getStatus().equals("APEN")
                    ? behandleSakConsumer.opprettSak(inntektsmelding.getFnr())
                    : sykepengesoknad.get().getSaksId();

            if (oppgave.isPresent() && oppgave.get().getStatus().equals("APEN")) {
                oppgavebehandlingConsumer.oppdaterOppgavebeskrivelse(oppgave.get(), "Det har kommet en inntektsmelding på sykepenger.");
                log.info("Fant eksisterende åpen oppgave. Oppdaterete beskrivelsen.");
            } else {
                saksbehandlingService.opprettOppgave(inntektsmelding.getFnr(), byggOppgave(journalpostId, saksId));
                log.info("Fant ikke eksisterende åpen oppgave. Opprettet ny oppgave.");
            }

            ferdigstillJournalpost(journalpostId, inntektsmelding, saksId);

            log.info("Behandlet melding om inntektskjema - journalpost: {}", journalpostId);

        } catch (JMSException e) {
            log.error("Feil med parsing av inntektsmelding fra kø", e);
            throw new RuntimeException("Feil ved lesing av melding", e);
        }
    }

    private void ferdigstillJournalpost(String journalpostId, Inntektsmelding inntektsmelding, String saksId) {
        InngaendeJournalpost journalpost = journalpostService.hentInngaendeJournalpost(journalpostId, saksId, inntektsmelding);
        behandleInngaaendeJournalConsumer.oppdaterJournalpost(journalpost);
        behandleInngaaendeJournalConsumer.ferdigstillJournalpost(journalpost);
    }

    private Oppgave byggOppgave(String journalpostId, String saksId) {
        return Oppgave.builder()
                .journalpostId(journalpostId)
                .gsakSaksid(saksId)
                .beskrivelse("Det har kommet en inntektsmelding på sykepenger.")
                .aktivTil(LocalDate.now().plusDays(7))
                .build();
    }
}
