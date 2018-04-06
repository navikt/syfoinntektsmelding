package no.nav.syfo.web.selftest;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.InngaaendeJournalConsumer;
import no.nav.syfo.consumer.ws.JournalConsumer;
import no.nav.syfo.domain.InngaaendeJournalpost;
import no.seres.xsd.nav.inntektsmelding_m._20171205.InntektsmeldingM;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/internal")
public class SelftestController {
    private static final String APPLICATION_LIVENESS = "Application is alive!";
    private static final String APPLICATION_READY = "Application is ready!";

    private InngaaendeJournalConsumer inngaaendeJournalConsumer;
    private JournalConsumer journalConsumer;

    public SelftestController(InngaaendeJournalConsumer inngaaendeJournalConsumer, JournalConsumer journalConsumer) {
        this.inngaaendeJournalConsumer = inngaaendeJournalConsumer;
        this.journalConsumer = journalConsumer;
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
    @RequestMapping(value = "/hentInntektsmelding/{journalpost}/{dokumentid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public InntektsmeldingM hentInntektsmelding(@PathVariable String journalpost, @PathVariable String dokumentid) {
        return journalConsumer.hentXmlDokument(journalpost, dokumentid);
    }

}
