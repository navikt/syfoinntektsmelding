package no.nav.syfo.consumer.mq

import any
import eq
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandling.FantIkkeAktørException
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.Historikk
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.FeiletEntitet
import no.nav.syfo.repository.FeiletService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.util.Metrikk
import org.apache.activemq.command.ActiveMQTextMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(MockitoJUnitRunner::class)
class InntektsmeldingConsumerTest {

    @Mock
    private lateinit var metrikk: Metrikk
    @Mock
    private lateinit var oppgaveClient: OppgaveClient
    @Mock
    private lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @InjectMocks
    private lateinit var inntektsmeldingConsumer: InntektsmeldingConsumer

    @Mock
    private lateinit var feiletService: FeiletService

    @Mock
    private lateinit var behandlendeEnhetConsumer: BehandlendeEnhetConsumer

    @Mock
    private lateinit var journalpostService: JournalpostService

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
    val AR_AKTØR_EXCEPTION = "AR-EXCEPTION"

    @Before
    fun setup() {
        val days1 = FeiletEntitet(arkivReferanse = AR_YESTERDAY, feiltype = Feiltype.AKTØR_FEIL, tidspunkt = LocalDateTime.now().minusDays(1))
        val days8 = FeiletEntitet(arkivReferanse = AR_EIGHT_DAYS, feiltype = Feiltype.AKTØR_FEIL, tidspunkt = LocalDateTime.now().minusDays(8))
        val days14 = FeiletEntitet(arkivReferanse = AR_TWO_WEEKS, feiltype = Feiltype.AKTØR_FEIL, tidspunkt = LocalDateTime.now().minusDays(14))

        Mockito.`when`(feiletService.finnHistorikk(AR)).thenReturn(Historikk(AR, emptyList()))
        Mockito.`when`(feiletService.finnHistorikk(AR_YESTERDAY)).thenReturn(Historikk(AR_YESTERDAY, listOf(days1)))
        Mockito.`when`(feiletService.finnHistorikk(AR_MISSING)).thenReturn(Historikk(AR_MISSING, emptyList()))
        Mockito.`when`(feiletService.finnHistorikk(AR_TWO_WEEKS)).thenReturn(Historikk(AR_TWO_WEEKS, listOf(days14)))
        Mockito.`when`(feiletService.finnHistorikk(AR_EXCEPTION)).thenReturn(Historikk(AR_EXCEPTION, emptyList()))
        Mockito.`when`(inntektsmeldingBehandler.behandle(any(), eq(AR_AKTØR_EXCEPTION))).thenThrow(FantIkkeAktørException())

        val im = Inntektsmelding(
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

        Mockito.`when`(journalpostService.hentInntektsmelding(any())).thenReturn(im)
        Mockito.`when`(behandlendeEnhetConsumer.hentBehandlendeEnhet(any())).thenReturn(im.fnr)
    }

    @Test
    fun `ta ut av kø dersom alle data er gyldig`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR
        inntektsmeldingConsumer.listen(message)
        verify(inntektsmeldingBehandler).behandle("arkivId", AR)
        verify(feiletService, never()).lagreFeilet(any(), any())
    }

    @Test
    fun `ta ut av kø dersom arkivReferansen mangler`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        inntektsmeldingConsumer.listen(message)
        verify(inntektsmeldingBehandler).behandle("arkivId", AR_MISSING)
    }

    @Test
    fun `fjerne inntektsmeldingen fra kø dersom den har ligget for lenge`() {
            val message = ActiveMQTextMessage()
            message.text = inputPayload
            message.jmsCorrelationID = AR_TWO_WEEKS
            inntektsmeldingConsumer.listen(message)
            verify(inntektsmeldingBehandler, never()).behandle(any(), any())
            verify(metrikk).tellUtAvKø()
            verify(oppgaveClient).opprettFordelingsOppgave(any(), any(), any())
    }

    @Test
    fun `ikke fjerne inntektsmeldingen fra kø dersom den IKKE har ligget for lenge`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR_YESTERDAY
        inntektsmeldingConsumer.listen(message)
        verify(inntektsmeldingBehandler, never()).behandle(any(), any())
        verify(metrikk).tellUtAvKø()
    }

    @Test
    fun `lagre feil dersom det oppstår behandlingsfeil`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR_AKTØR_EXCEPTION
        try {
            inntektsmeldingConsumer.listen(message)
        } catch (ex: Exception) {
            verify(feiletService, times(1)).lagreFeilet(eq(AR_AKTØR_EXCEPTION), eq(Feiltype.AKTØR_IKKE_FUNNET))
        }
    }

}
