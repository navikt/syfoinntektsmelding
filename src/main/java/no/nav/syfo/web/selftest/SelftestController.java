package no.nav.syfo.web.selftest;

import io.prometheus.client.spring.web.PrometheusTimeMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
public class SelftestController {
    private static final String APPLICATION_LIVENESS = "Application is alive!";
    private static final String APPLICATION_READY = "Application is ready!";

    @ResponseBody
    @RequestMapping(value = "/isAlive", produces = MediaType.TEXT_PLAIN_VALUE)
    @PrometheusTimeMethod(name="isAlive", help = "Kaller isAlive")
    public String isAlive() {
        return APPLICATION_LIVENESS;
    }

    @ResponseBody
    @RequestMapping(value = "/isReady", produces = MediaType.TEXT_PLAIN_VALUE)
    @PrometheusTimeMethod(name="isReady", help = "Kaller isReady")
    public String isReady() {
        return APPLICATION_READY;
    }
}
