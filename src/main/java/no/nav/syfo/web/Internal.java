package no.nav.syfo.web;

import no.nav.syfo.domain.Sykmelding;
import no.nav.syfo.repository.SykmeldingDAO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/internal")
public class Internal {

    private SykmeldingDAO sykmeldingDAO;

    public Internal(SykmeldingDAO sykmeldingDAO) {
        this.sykmeldingDAO = sykmeldingDAO;
    }

    @RequestMapping(value = "/sykmelding/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Sykmelding finnSykmelding(@PathVariable String id) {
            return sykmeldingDAO.finnSykmelding(id);
    }
}
