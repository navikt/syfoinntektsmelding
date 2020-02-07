package no.nav.syfo.consumer.mq

import any
import eq
import no.nav.syfo.behandling.AktørException
import no.nav.syfo.behandling.BehandlingException
import no.nav.syfo.behandling.FantIkkeAktørException
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.Historikk
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.dto.FeiletEntitet
import no.nav.syfo.repository.FeiletService
import no.nav.syfo.util.Metrikk
import org.apache.activemq.command.ActiveMQTextMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDateTime
import javax.jms.MessageNotWriteableException
import kotlin.math.exp

@RunWith(MockitoJUnitRunner::class)
class InntektsmeldingConsumerTest {

    @Mock
    private lateinit var metrikk: Metrikk

    @Mock
    private lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @InjectMocks
    private lateinit var inntektsmeldingConsumer: InntektsmeldingConsumer

    @Mock
    private lateinit var feiletService: FeiletService

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

    val AR = "NO_ERRORS"
    val AR_MISSING = "UKJENT"
    val AR_YESTERDAY = "AR-666"
    val AR_EIGHT_DAYS = "AR-777"
    val AR_TWO_WEEKS = "AR-666"
    val AR_EXCEPTION = "AR-EXCEPTION"

    @Before
    fun setup() {
        val days1 = FeiletEntitet(arkivReferanse=AR_YESTERDAY, feiltype = Feiltype.AKTØR_FEIL, tidspunkt = LocalDateTime.now().minusDays(1))
        val days8 = FeiletEntitet(arkivReferanse=AR_EIGHT_DAYS, feiltype = Feiltype.AKTØR_FEIL, tidspunkt = LocalDateTime.now().minusDays(8))
        val days14 = FeiletEntitet(arkivReferanse=AR_TWO_WEEKS, feiltype = Feiltype.AKTØR_FEIL, tidspunkt = LocalDateTime.now().minusDays(14))

        Mockito.`when`(feiletService.finnHistorikk(AR)).thenReturn(Historikk(AR, emptyList()))
        Mockito.`when`(feiletService.finnHistorikk(AR_YESTERDAY)).thenReturn(Historikk(AR_YESTERDAY, listOf(days1)))
        Mockito.`when`(feiletService.finnHistorikk(AR_MISSING)).thenReturn(Historikk(AR_MISSING, emptyList()))
        Mockito.`when`(feiletService.finnHistorikk(AR_TWO_WEEKS)).thenReturn(Historikk(AR_TWO_WEEKS, listOf(days14)))
        Mockito.`when`(feiletService.finnHistorikk(AR_EIGHT_DAYS)).thenReturn(Historikk(AR_EIGHT_DAYS, listOf(days8)))
        Mockito.`when`(feiletService.finnHistorikk(AR_EXCEPTION)).thenReturn(Historikk(AR_EXCEPTION, emptyList()))
        Mockito.`when`(inntektsmeldingBehandler.behandle(any(), eq(AR_EXCEPTION))).thenThrow(FantIkkeAktørException())
    }

    /**
     * behandlingsfeil - ja eller nei
     * ut av kø - for gammel
     *
     */

    @Test
    @Throws(MessageNotWriteableException::class)
    fun `Skal behandle og ta ut av kø dersom alle data er gyldig`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR
        inntektsmeldingConsumer.listen(message)
        verify(inntektsmeldingBehandler).behandle("arkivId", AR)
        verify(feiletService, never()).lagreFeilet(any(), any())
    }

    @Test
    @Throws(MessageNotWriteableException::class)
    fun `Skal behandle og ta ut av kø dersom arkivReferansen mangler`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        inntektsmeldingConsumer.listen(message)
        verify(inntektsmeldingBehandler).behandle("arkivId", AR_MISSING)
    }

    @Test(expected = FantIkkeAktørException::class)
    fun `Skal lagre behandlingsfeil dersom det oppstår Exceptions`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR_EXCEPTION
        inntektsmeldingConsumer.listen(message)
        verify(feiletService).lagreFeilet("arkivId", Feiltype.AKTØR_IKKE_FUNNET)
    }

    @Test
    @Throws(MessageNotWriteableException::class)
    fun `Skal fjerne inntektsmeldingen fra kø dersom den har ligget for lenge`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR_TWO_WEEKS
        inntektsmeldingConsumer.listen(message)
        verify(inntektsmeldingBehandler, never()).behandle(any(), any())
        verify(metrikk).tellUtAvKø()
    }

    @Test
    fun `Skal ikke fjerne inntektsmeldingen fra kø dersom den IKKE har ligget for lenge`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR_YESTERDAY
        inntektsmeldingConsumer.listen(message)
        verify(inntektsmeldingBehandler, never()).behandle(any(), any())
        verify(metrikk).tellUtAvKø()
    }

}
