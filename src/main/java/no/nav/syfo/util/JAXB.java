package no.nav.syfo.util;

import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

import static java.lang.Boolean.TRUE;
import static javax.xml.bind.JAXBContext.newInstance;
import static javax.xml.bind.Marshaller.*;

public final class JAXB {

    private static final JAXBContext FORSENDELSESINFORMASJON;
    private static final JAXBContext INNTEKTSMELDING;

    static {
        try {
            FORSENDELSESINFORMASJON = newInstance(
                    ObjectFactory.class
            );
            INNTEKTSMELDING = newInstance(
                    no.seres.xsd.nav.inntektsmelding_m._20181211.ObjectFactory.class
            );
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static String marshallForsendelsesinformasjon(Object element) {
        try {
            StringWriter writer = new StringWriter();
            Marshaller marshaller = FORSENDELSESINFORMASJON.createMarshaller();
            marshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
            marshaller.setProperty(JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(JAXB_FRAGMENT, true);
            marshaller.marshal(element, new StreamResult(writer));
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T unmarshalForsendelsesinformasjon(String melding) {
        try {
            return (T) FORSENDELSESINFORMASJON.createUnmarshaller().unmarshal(new StringReader(melding));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T unmarshalInntektsmelding(String melding) {
        try {
            return (T) INNTEKTSMELDING.createUnmarshaller().unmarshal(new StringReader(melding));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}
