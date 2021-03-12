package no.nav.syfo.consumer.util.ws

import no.nav.syfo.util.MDCOperations.Companion.getFromMDC
import no.nav.syfo.util.MDCOperations.Companion.MDC_CALL_ID
import no.nav.syfo.util.MDCOperations.Companion.generateCallId
import org.apache.cxf.phase.AbstractPhaseInterceptor
import kotlin.Throws
import org.apache.cxf.jaxb.JAXBDataBinding
import org.apache.cxf.binding.soap.SoapMessage
import javax.xml.bind.JAXBException
import no.nav.syfo.consumer.util.ws.CallIdHeader
import org.apache.cxf.binding.soap.SoapHeader
import org.apache.cxf.interceptor.Fault
import org.apache.cxf.message.Message
import org.apache.cxf.phase.Phase
import org.slf4j.LoggerFactory
import java.util.*
import javax.xml.namespace.QName

class CallIdHeader internal constructor() : AbstractPhaseInterceptor<Message?>(Phase.PRE_STREAM) {
    @Throws(Fault::class)
    override fun handleMessage(message: Message?) {
        try {
            val qName = QName("uri:no.nav.applikasjonsrammeverk", "callId")
           /* val callId = Optional.ofNullable(getFromMDC(MDC_CALL_ID))
                .orElse(generateCallId())*/
            val callId = getFromMDC(MDC_CALL_ID) ?: generateCallId()
            val header = SoapHeader(
                qName, callId, JAXBDataBinding(
                    String::class.java
                )
            )
            (message as SoapMessage).headers.add(header)
        } catch (ex: JAXBException) {
            logger.warn("Error while setting CallId header", ex)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CallIdHeader::class.java)
    }
}
