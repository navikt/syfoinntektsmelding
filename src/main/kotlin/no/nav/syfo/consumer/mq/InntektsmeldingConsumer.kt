package no.nav.syfo.consumer.mq

import log
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon
import no.nav.syfo.behandling.BehandlingException
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.repository.FeiletService
import no.nav.syfo.util.JAXB
import no.nav.syfo.util.MDCOperations.*
import no.nav.syfo.util.Metrikk
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Optional.ofNullable
import javax.jms.JMSException
import javax.jms.TextMessage
import javax.xml.bind.JAXBElement

@Component
class InntektsmeldingConsumer(
        private val metrikk: Metrikk,
        private val inntektsmeldingBehandler: InntektsmeldingBehandler,
        private val feiletService: FeiletService
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
        var feiltype = Feiltype.USPESIFISERT
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

            val historikk = feiletService.finnHistorikk(arkivReferanse)

            if (historikk.skalArkiveresForDato()){
            if (historikk.skalArkiveresForDato(LocalDateTime.now())){
                metrikk.tellUtAvKø()
            } else {
                inntektsmeldingBehandler.behandle(info.arkivId, arkivReferanse)
            }

        } catch (e: BehandlingException) {
            log.error("Feil ved behandling av inntektsmelding med arkivreferanse $arkivReferanse", e)
            feiltype = e.feiltype
            metrikk.tellBehandlingsfeil(e.feiltype)
            throw RuntimeException("Feil ved lesing av melding  med arkivreferanse $arkivReferanse", e)
        } catch (e: JMSException) {
            log.error("Feil ved parsing av inntektsmelding fra kø med arkivreferanse $arkivReferanse", e)
            feiltype = Feiltype.JMS
            metrikk.tellBehandlingsfeil(Feiltype.JMS)
            throw RuntimeException("Feil ved lesing av melding med arkivreferanse $arkivReferanse", e)
        } catch (e: Exception) {
            log.error("Det skjedde en feil ved journalføring med arkivreferanse $arkivReferanse", e)
            metrikk.tellBehandlingsfeil(Feiltype.USPESIFISERT)
            throw RuntimeException("Det skjedde en feil ved journalføring med arkivreferanse $arkivReferanse", e)
        } finally {
            remove(MDC_CALL_ID)
        }
        try{
            feiletService.lagreFeilet(arkivReferanse, feiltype)
        } catch (e: Exception){

        }
    }

}
