package no.nav.syfo.service;

public class PeriodeService {

    public static boolean erSendtInnsoknad = false;

    public boolean erSendtInnSoknadForPeriode() {
        return erSendtInnsoknad;
    }
}
