package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PeriodeService {

    public static boolean erSendtInnsoknad = false;

    public boolean erSendtInnSoknadForPeriode() {
        log.info("Er det sendt inn soknad for periode? {}", erSendtInnsoknad);
        return erSendtInnsoknad;
    }
}
