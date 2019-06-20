package no.nav.syfo.consumer.mq

import any
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.Inntektsmelding
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.repository.InntektsmeldingDAO
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import org.apache.activemq.command.ActiveMQTextMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import javax.jms.MessageNotWriteableException

@RunWith(MockitoJUnitRunner::class)
class InntektsmeldingConsumerTest {

    @Mock
    private val metrikk: Metrikk? = null

    @Mock
    private val journalpostService: JournalpostService? = null

    @Mock
    private val saksbehandlingService: SaksbehandlingService? = null

    @Mock
    private val aktorConsumer: AktorConsumer? = null

    @Mock
    private val inntektsmeldingDAO: InntektsmeldingDAO? = null

    @InjectMocks
    private val inntektsmeldingConsumer: InntektsmeldingConsumer? = null

    @Before
    fun setup() {
        `when`(aktorConsumer!!.getAktorId(anyString())).thenReturn("aktor")
    }

    @Test
    @Throws(MessageNotWriteableException::class)
    fun behandlerInntektsmelding() {
        `when`(journalpostService!!.hentInntektsmelding("arkivId")).thenReturn(
            Inntektsmelding(
                arbeidsforholdId = "",
                status = JournalStatus.MIDLERTIDIG,
                arbeidsgiverOrgnummer = "orgnummer",
                arbeidsgiverPrivat = null,
                journalpostId = "akrivId",
                fnr = "fnr",
                arbeidsgiverperioder = emptyList(),
                arsakTilInnsending = ""
            )
        )
        `when`(saksbehandlingService!!.behandleInntektsmelding(any(), anyString())).thenReturn("saksId")

        val message = ActiveMQTextMessage()
        message.text = inputPayload
        inntektsmeldingConsumer!!.listen(message)

        verify(saksbehandlingService).behandleInntektsmelding(any(), anyString())
        verify(journalpostService).ferdigstillJournalpost(any(), any())
    }

    @Test
    @Throws(MessageNotWriteableException::class)
    fun behandlerIkkeInntektsmeldingMedStatusForskjelligFraMidlertidig() {
        `when`(journalpostService!!.hentInntektsmelding("arkivId")).thenReturn(
            Inntektsmelding(
                arbeidsforholdId = "123",
                arsakTilInnsending = "",
                arbeidsgiverperioder = emptyList(),
                status = JournalStatus.ANNET,
                journalpostId = "arkivId",
                fnr = "fnr"
            )
        )

        val message = ActiveMQTextMessage()
        message.text = inputPayload
        inntektsmeldingConsumer!!.listen(message)

        verify<SaksbehandlingService>(saksbehandlingService, never()).behandleInntektsmelding(any(), anyString())
        verify(journalpostService, never()).ferdigstillJournalpost(any(), any())
    }

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
}
