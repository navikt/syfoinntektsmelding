package no.nav.syfo.web;

import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.repository.SykepengesoknadDAO;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@RestController()
@RequestMapping(value = "/internal")
public class Internal {

    private SykepengesoknadDAO sykepengesoknadDAO;

    public Internal(SykepengesoknadDAO sykepengesoknadDAO) {
        this.sykepengesoknadDAO = sykepengesoknadDAO;
    }

    @RequestMapping(value = "/sykepengesoknader/{aktoerId}/{orgnummer}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Sykepengesoknad> finnSykepengesoknader(@PathVariable String aktoerId, String orgnummer) {
        return sykepengesoknadDAO.finnSykepengesoknad(aktoerId, orgnummer);
    }
}
