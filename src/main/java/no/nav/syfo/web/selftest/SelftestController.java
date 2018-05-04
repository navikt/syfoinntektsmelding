package no.nav.syfo.web.selftest;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.repository.SykepengesoknadDAO;
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

    private final SykepengesoknadDAO sykepengesoknadDAO;

    @Inject
    public SelftestController(SykepengesoknadDAO sykepengesoknadDAO) {
        this.sykepengesoknadDAO = sykepengesoknadDAO;
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
    @RequestMapping(value = "/hetSykepengesoknad/{journalpostid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Sykepengesoknad hentSykepengesoknad(@PathVariable String journalpostid) {
        return sykepengesoknadDAO.finnSykepengesoknad(journalpostid).get();
    }

}
