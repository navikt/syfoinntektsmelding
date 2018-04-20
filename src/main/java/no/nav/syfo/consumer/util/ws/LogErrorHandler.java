package no.nav.syfo.consumer.util.ws;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.handler.soap.SOAPMessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.OperationInfo;
import org.slf4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public class LogErrorHandler implements SOAPHandler<SOAPMessageContext> {
    private static final Logger LOG = getLogger(LogErrorHandler.class);

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        if (context instanceof SOAPMessageContextImpl) {
            Message message = ((SOAPMessageContextImpl) context).getWrappedMessage();

            Throwable exception = message.getContent(Exception.class);


            if (exception instanceof Fault && exception.getCause() != null) {
                exception = exception.getCause();
            }

            LOG.error(beskrivelse(message).toString(), exception);
        }
        return true;
    }

    private StringBuilder beskrivelse(Message message) {
        StringBuilder beskrivelse = new StringBuilder();
        beskrivelse.append("Det oppstod en feil i WS-kallet");
        if (message.getExchange() != null) {
            Exchange exchange = message.getExchange();
            Service service = exchange.get(Service.class);
            if (service != null) {
                beskrivelse.append(" \'");
                beskrivelse.append(service.getName());
                OperationInfo opInfo = exchange.get(OperationInfo.class);
                if (opInfo != null) {
                    beskrivelse.append("#").append(opInfo.getName());
                }
                beskrivelse.append('\'');
            }
        }
        return beskrivelse.append(":");
    }

    @Override
    public void close(MessageContext context) {
    }
}