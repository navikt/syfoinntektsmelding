package no.nav.syfo.consumer.mq

import log
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.repository.FeiletService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.util.Metrikk
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.jms.TextMessage

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
            arkivReferanse = textMessage.jmsCorrelationID ?: "UKJENT"
            log.info("Fikk MQ melding om $arkivReferanse")
        } catch (e: Exception) {
            log.error("Det skjedde en feil med arkivreferanse $arkivReferanse", e)
            throw InntektsmeldingConsumerException(arkivReferanse, e, Feiltype.USPESIFISERT)
        }
    }
}
