package no.nav.syfo.ws;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;

import java.util.Arrays;
import java.util.Objects;

public class WsClient<T> {



    @SuppressWarnings("unchecked")
    public T createPort(String serviceUrl, Class<?> portType, PhaseInterceptor<? extends Message>... interceptors) {
        JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
        jaxWsProxyFactoryBean.setServiceClass(portType);
        jaxWsProxyFactoryBean.setAddress(Objects.requireNonNull(serviceUrl));
        T port = (T) jaxWsProxyFactoryBean.create();
        Client client = ClientProxy.getClient(port);
        Arrays.stream(interceptors).forEach(client.getOutInterceptors()::add);
        STSClientConfig.configureRequestSamlToken(port);
        return port;
    }

}
