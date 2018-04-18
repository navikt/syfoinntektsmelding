package no.nav.syfo.web;

import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.ObjectFactory;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLBehandlingstema;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLTema;
import no.nav.syfo.consumer.repository.SykepengesoknadDAO;
import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.util.JAXB;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.JAXBElement;
import java.util.List;

@RestController()
@RequestMapping(value = "/internal")
public class Internal {

    private SykepengesoknadDAO sykepengesoknadDAO;
    private JmsTemplate jmsTemplate;

    public Internal(SykepengesoknadDAO sykepengesoknadDAO, JmsTemplate jmsTemplate) {
        this.sykepengesoknadDAO = sykepengesoknadDAO;
        this.jmsTemplate = jmsTemplate;
    }

    @RequestMapping(value = "/inntektsmelding/ny/{arkivId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public String leggMeldingPaaKoe(@PathVariable String arkivId) {
        ObjectFactory objectFactory = new ObjectFactory();
        final JAXBElement<XMLForsendelsesinformasjon> forsendelsesinformasjon = objectFactory.createForsendelsesinformasjon(new XMLForsendelsesinformasjon()
                .withArkivId(arkivId)
                .withArkivsystem("JOARK")
                .withTema(new XMLTema().withValue("SYK"))
                .withBehandlingstema(new XMLBehandlingstema().withValue("ab0061")));

        MessageCreator messageCreator = session -> session.createTextMessage(JAXB.marshallForsendelsesinformasjon(forsendelsesinformasjon));

        jmsTemplate.send(messageCreator);

        return "Lagt " + arkivId + " på kø!";
    }

    @RequestMapping(value = "/sykepengesoknader/{aktoerId}/{orgnummer}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Sykepengesoknad> finnSykepengesoknader(@PathVariable String aktoerId, String orgnummer) {
        return sykepengesoknadDAO.finnSykepengesoknad(aktoerId, orgnummer);
    }
}
