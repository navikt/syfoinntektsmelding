package no.nav.syfo.consumer.util.ws

import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.message.Message
import org.apache.cxf.phase.PhaseInterceptor
import org.apache.cxf.ws.addressing.WSAddressingFeature
import java.util.*
import javax.xml.ws.BindingProvider
import javax.xml.ws.handler.Handler

class WsClient<T> {
    fun createPort(
        serviceUrl: String,
        portType: Class<*>?,
        handlers: List<Handler<*>?>?,
        vararg interceptors: PhaseInterceptor<out Message?>?
    ): T {
        val jaxWsProxyFactoryBean = JaxWsProxyFactoryBean()
        jaxWsProxyFactoryBean.serviceClass = portType
        jaxWsProxyFactoryBean.address = Objects.requireNonNull(serviceUrl)
        jaxWsProxyFactoryBean.features.add(WSAddressingFeature())
        val port = jaxWsProxyFactoryBean.create() as T
        (port as BindingProvider).binding.handlerChain = handlers
        val client = ClientProxy.getClient(port)
        Arrays.stream(interceptors)
            .forEach { e: PhaseInterceptor<out Message?>? -> client.outInterceptors.add(e) }
        client.outInterceptors.add(CallIdHeader())
        STSClientConfig.configureRequestSamlToken(port)
        return port
    }
}

class WsClientMock<T> {
    fun createPort(
        serviceUrl: String,
        portType: Class<*>?,
        handlers: List<Handler<*>?>?,
        vararg interceptors: PhaseInterceptor<out Message?>?
    ): T {
        val jaxWsProxyFactoryBean = JaxWsProxyFactoryBean()
        jaxWsProxyFactoryBean.serviceClass = portType
        jaxWsProxyFactoryBean.address = Objects.requireNonNull(serviceUrl)
        jaxWsProxyFactoryBean.features.add(WSAddressingFeature())
        val port = jaxWsProxyFactoryBean.create() as T
        (port as BindingProvider).binding.handlerChain = handlers
        val client = ClientProxy.getClient(port)
        Arrays.stream(interceptors)
            .forEach { e: PhaseInterceptor<out Message?>? -> client.outInterceptors.add(e) }
        client.outInterceptors.add(CallIdHeader())
        STSClientConfigMock.configureRequestSamlToken(port)
        return port
    }
}
