package no.nav.syfo.web.selftest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@RestController
@RequestMapping(value = "/internal")
public class SelftestController {
    private static final String APPLICATION_LIVENESS = "Application is alive!";
    private static final String APPLICATION_READY = "Application is ready!";

    @Autowired
    private JdbcTemplate template;

    @ResponseBody
    @RequestMapping(value = "/isAlive", produces = MediaType.TEXT_PLAIN_VALUE)
    public String isAlive() {
        checkDatabase();
        return APPLICATION_LIVENESS;
    }

    @ResponseBody
    @RequestMapping(value = "/isReady", produces = MediaType.TEXT_PLAIN_VALUE)
    public String isReady() {
        checkDatabase();
        return APPLICATION_READY;
    }

    private void checkDatabase(){
        template.execute("select now();");
        log.error("Klarte å koble mot databasen");
    }
}
