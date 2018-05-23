package no.nav.syfo.web.selftest;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.service.SaksbehandlingService;
import org.springframework.http.MediaType;
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

    @Inject
    public SelftestController(SaksbehandlingService saksbehandlingService) {
        this.saksbehandlingService = saksbehandlingService;
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
    @RequestMapping(value = "/behandleFakeInntektsmelding/", produces = MediaType.APPLICATION_JSON_VALUE)
    public String behandleFakeInntektsmelding() {
        log.info("Behandler fake inntektsmelding!");

        Inntektsmelding inntektsmelding = Inntektsmelding.builder()
                .arbeidsgiverOrgnummer("orgnummer")
                .journalpostId("journalpostid")
                .fnr("2202690062").build();

        return saksbehandlingService.behandleInntektsmelding(inntektsmelding);
    }

}
