package no.nav.syfo.consumer.mq

import com.google.common.util.concurrent.Striped
import log
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.Inntektsmelding
import no.nav.syfo.domain.InntektsmeldingMeta
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.repository.InntektsmeldingDAO
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.JAXB
import no.nav.syfo.util.MDCOperations.MDC_CALL_ID
import no.nav.syfo.util.MDCOperations.generateCallId
import no.nav.syfo.util.MDCOperations.putToMDC
import no.nav.syfo.util.MDCOperations.remove
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
    private val journalpostService: JournalpostService,
    private val saksbehandlingService: SaksbehandlingService,
    private val metrikk: Metrikk,
    private val inntektsmeldingDAO: InntektsmeldingDAO,
    private val aktorConsumer: AktorConsumer
) {
    private val consumerLocks = Striped.lock(8)
    private val log = log()

    @Transactional(transactionManager = "jmsTransactionManager")
    @JmsListener(
        id = "inntektsmelding_listener",
        containerFactory = "jmsListenerContainerFactory",
        destination = "inntektsmeldingQueue"
    )
    fun listen(message: Any) {
        try {
            val textMessage = message as TextMessage
            putToMDC(MDC_CALL_ID, ofNullable(textMessage.getStringProperty("callId")).orElse(generateCallId()))
            val xmlForsendelsesinformasjon =
                JAXB.unmarshalForsendelsesinformasjon<JAXBElement<XMLForsendelsesinformasjon>>(textMessage.text)
            val info = xmlForsendelsesinformasjon.value

            val inntektsmelding = journalpostService.hentInntektsmelding(info.arkivId)
            val consumerLock = consumerLocks.get(inntektsmelding.fnr)

            try {
                consumerLock.lock()
                val aktorid = aktorConsumer.getAktorId(inntektsmelding.fnr)

                if (JournalStatus.MIDLERTIDIG == inntektsmelding.status) {
                    metrikk.tellInntektsmeldingerMottatt(inntektsmelding)

                    val saksId = saksbehandlingService.behandleInntektsmelding(inntektsmelding, aktorid)

                    journalpostService.ferdigstillJournalpost(saksId, inntektsmelding)

                    lagreBehandling(inntektsmelding, aktorid, saksId)

                    log.info("Inntektsmelding {} er journalført", inntektsmelding.journalpostId)
                } else {
                    log.info(
                        "Behandler ikke inntektsmelding {} da den har status: {}",
                        inntektsmelding.journalpostId,
                        inntektsmelding.status
                    )
                }
            } finally {
                consumerLock.unlock()
            }
        } catch (e: JMSException) {
            log.error("Feil ved parsing av inntektsmelding fra kø", e)
            metrikk.tellInntektsmeldingfeil()
            throw RuntimeException("Feil ved lesing av melding", e)
        } catch (e: Exception) {
            log.error("Det skjedde en feil ved journalføring", e)
            metrikk.tellInntektsmeldingfeil()
            throw RuntimeException("Det skjedde en feil ved journalføring", e)
        } finally {
            remove(MDC_CALL_ID)
        }
    }

    private fun lagreBehandling(inntektsmelding: Inntektsmelding, aktorid: String, saksId: String) {
        inntektsmeldingDAO.opprett(
            InntektsmeldingMeta(
                orgnummer = inntektsmelding.arbeidsgiverOrgnummer,
                arbeidsgiverPrivat = inntektsmelding.arbeidsgiverPrivat,
                arbeidsgiverperioder = inntektsmelding.arbeidsgiverperioder,
                aktorId = aktorid,
                sakId = saksId,
                journalpostId = inntektsmelding.journalpostId,
                behandlet = LocalDateTime.now()
            )
        )
    }
}
