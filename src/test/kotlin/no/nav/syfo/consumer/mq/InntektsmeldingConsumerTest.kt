package no.nav.syfo.consumer.mq

import any
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.util.Metrikk
import org.apache.activemq.command.ActiveMQTextMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import javax.jms.MessageNotWriteableException

@RunWith(MockitoJUnitRunner::class)
class InntektsmeldingConsumerTest {

    @Mock
    private val metrikk: Metrikk? = null

    @Mock
    private lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @InjectMocks
    private lateinit var inntektsmeldingConsumer: InntektsmeldingConsumer

    companion object {
        private val inputPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "  <ns5:forsendelsesinformasjon xmlns:ns5=\"http://nav.no/melding/virksomhet/dokumentnotifikasjon/v1\" " +
                "    xmlns:ns2=\"http://nav.no/melding/virksomhet/dokumentforsendelse/v1\" " +
                "    xmlns:ns4=\"http://nav.no/dokmot/jms/reply\" " +
                "    xmlns:ns3=\"http://nav.no.dokmot/jms/viderebehandling\">" +
                "  <arkivId>arkivId</arkivId>" +
                "  <arkivsystem>JOARK</arkivsystem>" +
                "  <tema>SYK</tema>" +
                "  <behandlingstema>ab0061</behandlingstema>" +
                "</ns5:forsendelsesinformasjon>"
    }

    @Before
    fun setup() {
    }

    @Test
    @Throws(MessageNotWriteableException::class)
    fun listen() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = "AR-123"
        inntektsmeldingConsumer.listen(message)
        verify(inntektsmeldingBehandler).behandle("arkivId", "AR-123")
    }

}
