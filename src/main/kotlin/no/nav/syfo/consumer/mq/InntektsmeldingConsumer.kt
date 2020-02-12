package no.nav.syfo.consumer.mq

import kotlinx.coroutines.runBlocking
import log
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.XMLForsendelsesinformasjon
import no.nav.syfo.behandling.BehandlingException
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.repository.FeiletService
import no.nav.syfo.service.JournalpostService
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
    private val feiletService: FeiletService,
    private val oppgaveClient: OppgaveClient,
    private val journalpostService: JournalpostService,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer
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

            val historikk = feiletService.finnHistorikk(arkivReferanse)
            if (historikk.skalArkiveresForDato(LocalDateTime.now())){
                    opprettFordelingsoppgave(info.arkivId)
                    metrikk.tellUtAvKø()
            } else {
                inntektsmeldingBehandler.behandle(info.arkivId, arkivReferanse)
            }

        } catch (e: BehandlingException) {
            log.error("Feil ved behandling av inntektsmelding med arkivreferanse $arkivReferanse", e)
            metrikk.tellBehandlingsfeil(e.feiltype)
            lagreFeilet(arkivReferanse, e.feiltype)
            throw InntektsmeldingConsumerException(arkivReferanse, e, e.feiltype)
        } catch (e: JMSException) {
            log.error("Feil ved parsing av inntektsmelding fra kø med arkivreferanse $arkivReferanse", e)
            metrikk.tellBehandlingsfeil(Feiltype.JMS)
            lagreFeilet(arkivReferanse, Feiltype.JMS)
            throw InntektsmeldingConsumerException(arkivReferanse, e, Feiltype.JMS)
        } catch (e: Exception) {
            log.error("Det skjedde en feil ved journalføring med arkivreferanse $arkivReferanse", e)
            metrikk.tellBehandlingsfeil(Feiltype.USPESIFISERT)
            lagreFeilet(arkivReferanse, Feiltype.USPESIFISERT)
            throw InntektsmeldingConsumerException(arkivReferanse, e, Feiltype.USPESIFISERT)
        } finally {
            remove(MDC_CALL_ID)
        }
    }

    fun opprettFordelingsoppgave(journalpostId: String) {
        val inntektsmelding = journalpostService.hentInntektsmelding(journalpostId)
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(inntektsmelding.fnr)
        val gjelderUtland = ("4474" == behandlendeEnhet)
        runBlocking {
            oppgaveClient.opprettFordelingsOppgave(journalpostId, behandlendeEnhet, gjelderUtland)
        }
    }

    fun lagreFeilet(arkivReferanse: String, feiltype: Feiltype){
        try{
            feiletService.lagreFeilet(arkivReferanse, feiltype)
        } catch (e: Exception) {
            metrikk.tellLagreFeiletMislykkes();
        }
    }

}
