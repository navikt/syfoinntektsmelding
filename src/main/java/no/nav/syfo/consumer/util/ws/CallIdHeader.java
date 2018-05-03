package no.nav.syfo.consumer.util.ws;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

public class CallIdHeader extends AbstractPhaseInterceptor<Message> {

    private static final Logger logger = LoggerFactory.getLogger(CallIdHeader.class);

    public CallIdHeader() {
        super(Phase.PRE_STREAM);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        try {
            QName qName = new QName("uri:no.nav.applikasjonsrammeverk", "callId");
            SoapHeader header = new SoapHeader(qName, randomValue(), new JAXBDataBinding(String.class));
            ((SoapMessage) message).getHeaders().add(header);
        } catch (JAXBException ex) {
            logger.warn("Error while setting CallId header", ex);
        }
    }

    private String randomValue() {
        return "syfomottoakoppslag-" + (int) (Math.random() * 10000);
    }

}
