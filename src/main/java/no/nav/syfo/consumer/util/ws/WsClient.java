package no.nav.syfo.consumer.util.ws;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.addressing.WSAddressingFeature;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class WsClient<T> {

    @SuppressWarnings("unchecked")
    public T createPort(String serviceUrl, Class<?> portType, List<Handler> handlers, PhaseInterceptor<? extends Message>... interceptors) {
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(portType);
        jaxWsProxyFactoryBean.setAddress(Objects.requireNonNull(serviceUrl));
        jaxWsProxyFactoryBean.getFeatures().add(new WSAddressingFeature());
        T port = (T) jaxWsProxyFactoryBean.create();
        ((BindingProvider) port).getBinding().setHandlerChain(handlers);
        Client client = ClientProxy.getClient(port);
        Arrays.stream(interceptors).forEach(client.getOutInterceptors()::add);
        STSClientConfig.configureRequestSamlToken(port);
        return port;
    }

}
