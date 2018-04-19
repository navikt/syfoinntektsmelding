package no.nav.syfo.web.selftest;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.BehandleSakConsumer;
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.JournalConsumer;
import no.nav.syfo.domain.InngaaendeJournalpost;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.service.SaksbehandlingService;
import no.seres.xsd.nav.inntektsmelding_m._20171205.InntektsmeldingM;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping(value = "/internal")
public class SelftestController {
    private static final String APPLICATION_LIVENESS = "Application is alive!";
    private static final String APPLICATION_READY = "Application is ready!";

    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    private JournalConsumer journalConsumer;
    private SaksbehandlingService saksbehandlingService;
    private BehandleSakConsumer behandleSakConsumer;

    public SelftestController(InngaaendeJournalConsumer inngaaendeJournalConsumer, JournalConsumer journalConsumer, SaksbehandlingService behandleSak, BehandleSakConsumer behandleSakConsumer) {
        this.inngaaendeJournalConsumer = inngaaendeJournalConsumer;
        this.journalConsumer = journalConsumer;
        this.saksbehandlingService = behandleSak;
        this.behandleSakConsumer = behandleSakConsumer;
    }

    @ResponseBody
    @RequestMapping(value = "/isAlive", produces = MediaType.TEXT_PLAIN_VALUE)
    public String isAlive() {
        return APPLICATION_LIVENESS;
    }

    @ResponseBody
    @RequestMapping(value = "/isReady", produces = MediaType.TEXT_PLAIN_VALUE)
    public String isReady() {
        return APPLICATION_READY;
    }

    @ResponseBody
    @RequestMapping(value = "/hentJournalpost/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public InngaaendeJournalpost hentJournalpost(@PathVariable String id) {
        return inngaaendeJournalConsumer.hentJournalpost(id);
    }

    @ResponseBody
    @RequestMapping(value = "/hentInntektsmelding/{journalpost}/{dokumentid}/{variantFormat}", produces = MediaType.APPLICATION_JSON_VALUE)
    public InntektsmeldingM hentInntektsmelding(@PathVariable String journalpost, @PathVariable String dokumentid, @PathVariable String variantFormat) {
        return journalConsumer.hentXmlDokument(journalpost, dokumentid, variantFormat);
    }

    @ResponseBody
    @RequestMapping(value = "/gsakTest", produces = MediaType.TEXT_PLAIN_VALUE)
    public String gsakTest() {
        String gsakSaksid = behandleSakConsumer.opprettSak("12345678910");
        Oppgave oppgave = Oppgave.builder()
                .beskrivelse("Beskrivelse")
                .gsakSaksid(gsakSaksid)
                .journalpostId("journalpostId")
                .aktivTil(LocalDate.now().plusDays(7))
                .build();
        String oppgaveId = saksbehandlingService.opprettOppgave(
                "12345678910",
                oppgave
        );

        return "gsak saksid: " + gsakSaksid + "\noppgave id: " + oppgaveId;
    }

}
