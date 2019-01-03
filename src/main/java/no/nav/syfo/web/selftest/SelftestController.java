package no.nav.syfo.web.selftest;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.rest.AktorConsumer;
import no.nav.syfo.consumer.rest.TokenConsumer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/internal")
public class SelftestController {
    private static final String APPLICATION_LIVENESS = "Application is alive!";
    private static final String APPLICATION_READY = "Application is ready!";

    private AktorConsumer aktorConsumer;

    public SelftestController(AktorConsumer aktorConsumer) {
        this.aktorConsumer = aktorConsumer;
    }

    @ResponseBody
    @RequestMapping(value = "/aktor/{fnr}")
    public String aktor(@PathVariable String fnr) {
        return aktorConsumer.getAktorId(fnr);
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

}
