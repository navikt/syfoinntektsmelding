package no.nav.syfo.consumer.mq

import log
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon
import no.nav.syfo.api.BehandlingException
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.util.JAXB
import no.nav.syfo.util.MDCOperations.*
import no.nav.syfo.util.Metrikk
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.Optional.ofNullable
import javax.jms.JMSException
import javax.jms.TextMessage
import javax.xml.bind.JAXBElement

@Component
class InntektsmeldingConsumer(
        private val metrikk: Metrikk,
        private val inntektsmeldingBehandler: InntektsmeldingBehandler
) {
    private val log = log()

    @Transactional(transactionManager = "jmsTransactionManager")
    @JmsListener(
            id = "inntektsmelding_listener",
            containerFactory = "jmsListenerContainerFactory",
            destination = "inntektsmeldingQueue"
    )
    fun listen(message: Any) {
        var arkivReferanse = "UKJENT"
        try {
            val textMessage = message as TextMessage
            putToMDC(MDC_CALL_ID, ofNullable(textMessage.getStringProperty("callId")).orElse(generateCallId()))
            val xmlForsendelsesinformasjon =
                    JAXB.unmarshalForsendelsesinformasjon<JAXBElement<XMLForsendelsesinformasjon>>(textMessage.text)
            val info = xmlForsendelsesinformasjon.value
            arkivReferanse = textMessage.jmsCorrelationID ?: "UKJENT"
            if (textMessage.jmsCorrelationID == null) {
                metrikk.tellInntektsmeldingUtenArkivReferanse()
            }
            inntektsmeldingBehandler.behandle(info.arkivId, arkivReferanse)
        } catch (e: BehandlingException) {
            log.error("Feil ved behandling av inntektsmelding med arkivreferanse $arkivReferanse", e)
            metrikk.tellBehandlingsfeil(e.feiltype)
            throw RuntimeException("Feil ved lesing av melding  med arkivreferanse $arkivReferanse", e)
        } catch (e: JMSException) {
            log.error("Feil ved parsing av inntektsmelding fra kø med arkivreferanse $arkivReferanse", e)
            metrikk.tellInntektsmeldingfeil()
            throw RuntimeException("Feil ved lesing av melding med arkivreferanse $arkivReferanse", e)
        } catch (e: Exception) {
            log.error("Det skjedde en feil ved journalføring med arkivreferanse $arkivReferanse", e)
            metrikk.tellInntektsmeldingfeil()
            throw RuntimeException("Det skjedde en feil ved journalføring med arkivreferanse $arkivReferanse", e)
        } finally {
            remove(MDC_CALL_ID)
        }
    }

}
