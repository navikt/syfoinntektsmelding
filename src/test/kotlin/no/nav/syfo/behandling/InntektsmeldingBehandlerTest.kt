package no.nav.syfo.behandling

import any
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingDto
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingService
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
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.time.LocalDateTime
import javax.jms.MessageNotWriteableException

@RunWith(MockitoJUnitRunner::class)
class InntektsmeldingBehandlerTest {

    @Mock
    private val metrikk: Metrikk? = null

    @Mock
    private lateinit var journalpostService: JournalpostService

    @Mock
    private lateinit var saksbehandlingService: SaksbehandlingService

    @Mock
    private lateinit var aktorConsumer: AktorConsumer

    @Mock
    private lateinit var inntektsmeldingService: InntektsmeldingService

    @Mock
    private val inntektsmeldingProducer: InntektsmeldingProducer? = null

    @InjectMocks
    private lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @Before
    fun setup() {
        `when`(aktorConsumer.getAktorId("fnr")).thenReturn("aktorId") // inntektsmelding.fnr
        `when`(saksbehandlingService.behandleInntektsmelding(any(), matches("aktorId"), matches("AR-123"))).thenReturn("saksId")
        `when`(inntektsmeldingService.lagreBehandling(any(), anyString(), anyString(), anyString())).thenReturn(
                InntektsmeldingDto(
                        orgnummer = "orgnummer",
                        arbeidsgiverPrivat = "123",
                        aktorId = "aktorId",
                        journalpostId = "arkivId",
                        sakId = "saksId",
                        behandlet = LocalDateTime.now()
                )
        )
    }

    @Test
    @Throws(MessageNotWriteableException::class)
    fun behandlerInntektsmelding() {
        `when`(journalpostService.hentInntektsmelding("arkivId")).thenReturn(
                Inntektsmelding(
                        arbeidsgiverOrgnummer = "orgnummer",
                        arbeidsgiverPrivatFnr = null,
                        arbeidsforholdId = "",
                        fnr = "fnr",
                        journalpostId = "arkivId",
                        journalStatus = JournalStatus.MIDLERTIDIG,
                        arbeidsgiverperioder = emptyList(),
                        arkivRefereranse = "AR-123",
                        mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay(),
                        arsakTilInnsending = "",
                        førsteFraværsdag = LocalDate.now()
                )
        )
        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        verify(saksbehandlingService).behandleInntektsmelding(any(), anyString(), anyString())
        verify(journalpostService).ferdigstillJournalpost(matches("saksId"), any())
        verify(inntektsmeldingProducer!!).leggMottattInntektsmeldingPåTopic(any())
    }

    @Test
    @Throws(MessageNotWriteableException::class)
    fun behandlerIkkeInntektsmeldingMedStatusForskjelligFraMidlertidig() {
        `when`(journalpostService.hentInntektsmelding("arkivId")).thenReturn(
                Inntektsmelding(
                        arkivRefereranse = "AR-123",
                        arbeidsforholdId = "123",
                        arsakTilInnsending = "",
                        arbeidsgiverperioder = emptyList(),
                        journalStatus = JournalStatus.ANNET,
                        journalpostId = "arkivId",
                        mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay(),
                        fnr = "fnr",
                        førsteFraværsdag = LocalDate.now()
                )
        )

        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        verify<SaksbehandlingService>(saksbehandlingService, never()).behandleInntektsmelding(any(), anyString(), anyString())
        verify(journalpostService, never()).ferdigstillJournalpost(any(), any())
    }

    @Test
    @Throws(MessageNotWriteableException::class)
    fun behandlerIkkeInntektsmeldingMedStatusEndelig() {
        `when`(journalpostService.hentInntektsmelding("arkivId")).thenReturn(
                Inntektsmelding(
                        arkivRefereranse = "AR-123",
                        arbeidsforholdId = "123",
                        arsakTilInnsending = "",
                        arbeidsgiverperioder = emptyList(),
                        mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay(),
                        journalStatus = JournalStatus.ENDELIG,
                        journalpostId = "arkivId",
                        fnr = "fnr",
                        førsteFraværsdag = LocalDate.now()
                )
        )

        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = "AR-123"
        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        verify<SaksbehandlingService>(saksbehandlingService, never()).behandleInntektsmelding(any(), anyString(), anyString())
        verify(journalpostService, never()).ferdigstillJournalpost(any(), any())
        verify(inntektsmeldingProducer!!, never()).leggMottattInntektsmeldingPåTopic(any())
    }

    @Test
    @Throws(MessageNotWriteableException::class)
    fun inntektsmelding_uten_arkivreferanse() {
        `when`(journalpostService.hentInntektsmelding("arkivId")).thenReturn(
                Inntektsmelding(
                        arkivRefereranse = "AR-123",
                        arbeidsforholdId = "123",
                        arsakTilInnsending = "",
                        arbeidsgiverperioder = emptyList(),
                        mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay(),
                        journalStatus = JournalStatus.MIDLERTIDIG,
                        journalpostId = "arkivId",
                        fnr = "fnr",
                        førsteFraværsdag = LocalDate.now()
                )
        )

        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

//        verify<SaksbehandlingService>(saksbehandlingService, never()).behandleInntektsmelding(any(), anyString(), anyString())
//        verify(journalpostService, never()).ferdigstillJournalpost(any(), any())
//        verify(inntektsmeldingProducer!!, never()).leggMottattInntektsmeldingPåTopic(any())
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

