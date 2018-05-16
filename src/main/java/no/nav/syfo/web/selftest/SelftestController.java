package no.nav.syfo.web.selftest;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.mq.InntektsmeldingConsumer;
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer;
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

    InntektsmeldingConsumer inntektsmeldingConsumer;
    private BehandlendeEnhetConsumer behandlendeEnhetConsumer;

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
    @RequestMapping(value = "/leggFakeInntektsmelingPaKo/{journalpostid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void hentSykepengesoknad(@PathVariable String journalpostid) {
    }

    @ResponseBody
    @RequestMapping(value = "/testEndepunkt", produces = MediaType.APPLICATION_JSON_VALUE)
    public String testEndepunkt() {
        return behandlendeEnhetConsumer.finnBehandlendeEnhetListe();
    }

}
