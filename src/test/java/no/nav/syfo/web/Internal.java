package no.nav.syfo.web;

import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.ObjectFactory;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLBehandlingstema;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLTema;
import no.nav.syfo.util.JAXB;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.JAXBElement;

@RestController()
@RequestMapping(value = "/internal")
public class Internal {

    private JmsTemplate jmsTemplate;

    public Internal(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @RequestMapping(value = "/inntektsmelding/ny/{arkivId}", produces = MediaType.APPLICATION_JSON_VALUE)
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
}
