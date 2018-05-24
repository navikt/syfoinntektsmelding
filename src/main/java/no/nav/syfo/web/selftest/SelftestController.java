package no.nav.syfo.web.selftest;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.OppgaveConsumer;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.service.SaksbehandlingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@Slf4j
@RestController
@RequestMapping(value = "/internal")
public class SelftestController {
    private static final String APPLICATION_LIVENESS = "Application is alive!";
    private static final String APPLICATION_READY = "Application is ready!";

    private SaksbehandlingService saksbehandlingService;
    private OppgaveConsumer oppgaveConsumer;

    @Inject
    public SelftestController(SaksbehandlingService saksbehandlingService, OppgaveConsumer oppgaveConsumer) {
        this.saksbehandlingService = saksbehandlingService;
        this.oppgaveConsumer = oppgaveConsumer;
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

    // TODO: fjern denne før deploy
    @ResponseBody
    @RequestMapping(value = "/behandleFakeInntektsmelding/{orgnr}/{journalpostnr}/{fnr}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String behandleFakeInntektsmelding(@PathVariable String orgnr, @PathVariable String journalpostnr, @PathVariable String fnr) {
        log.info("Behandler fake inntektsmelding!");

        Inntektsmelding inntektsmelding = Inntektsmelding.builder()
                .arbeidsgiverOrgnummer(orgnr)
                .journalpostId(journalpostnr)
                .fnr(fnr).build();

        return saksbehandlingService.behandleInntektsmelding(inntektsmelding);
    }

    // TODO: fjern denne før deploy
    @ResponseBody
    @RequestMapping(value = "/finnOppgave/{oppgaveid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Oppgave finnOppgave(@PathVariable String oppgaveid) {
        log.info("Henter oppgave!");

        return oppgaveConsumer.finnOppgave(oppgaveid).get();
    }
}
