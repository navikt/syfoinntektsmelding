package no.nav.syfo.consumer.util.ws

import org.apache.cxf.interceptor.Fault
import org.apache.cxf.jaxws.handler.soap.SOAPMessageContextImpl
import org.apache.cxf.message.Message
import org.apache.cxf.service.Service
import org.apache.cxf.service.model.OperationInfo
import org.slf4j.LoggerFactory
import javax.xml.namespace.QName
import javax.xml.ws.handler.MessageContext
import javax.xml.ws.handler.soap.SOAPHandler
import javax.xml.ws.handler.soap.SOAPMessageContext

class LogErrorHandler : SOAPHandler<SOAPMessageContext?> {
    override fun getHeaders(): Set<QName>? {
        return null
    }

    override fun handleMessage(context: SOAPMessageContext?): Boolean {
        return true
    }

    override fun handleFault(context: SOAPMessageContext?): Boolean {
        if (context is SOAPMessageContextImpl) {
            val message = context.wrappedMessage
            var exception: Throwable = message.getContent(Exception::class.java)
            if (exception is Fault && exception.cause != null) {
                exception = exception.cause!!
            }
            LOG.error(beskrivelse(message).toString(), exception)
        }
        return true
    }

    private fun beskrivelse(message: Message): StringBuilder {
        val beskrivelse = StringBuilder()
        beskrivelse.append("Det oppstod en feil i WS-kallet")
        if (message.exchange != null) {
            val exchange = message.exchange
            val service = exchange.get(Service::class.java)
            if (service != null) {
                beskrivelse.append(" \'")
                beskrivelse.append(service.name)
                val opInfo = exchange.get(
                    OperationInfo::class.java
                )
                if (opInfo != null) {
                    beskrivelse.append("#").append(opInfo.name)
                }
                beskrivelse.append('\'')
            }
        }
        return beskrivelse.append(":")
    }

    override fun close(context: MessageContext) {}

    companion object {
        private val LOG = LoggerFactory.getLogger(LogErrorHandler::class.java)
    }
}
