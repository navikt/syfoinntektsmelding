package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.repository.SykepengesoknadDAO;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class PeriodeService {

    private SykepengesoknadDAO sykepengesoknadDAO;

    public PeriodeService(SykepengesoknadDAO sykepengesoknadDAO) {
        this.sykepengesoknadDAO = sykepengesoknadDAO;
    }

    public boolean erSendtInnSoknadForPeriode(String journalpostId) {
        Optional<Sykepengesoknad> sykepengesoknad = sykepengesoknadDAO.finnSykepengesoknad(journalpostId);
        boolean status = sykepengesoknad.isPresent() && sykepengesoknad.get().getStatus().equals("APEN");
        log.info("Er sendt inn søknad for periode: {}", status);
        return status;
    }
}
