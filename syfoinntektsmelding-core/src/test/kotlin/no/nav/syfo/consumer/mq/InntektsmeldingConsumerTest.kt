package no.nav.syfo.consumer.mq

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.behandling.FantIkkeAktørException
import no.nav.syfo.behandling.Feiltype
import no.nav.syfo.behandling.Historikk
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.FeiletEntitet
import no.nav.syfo.repository.FeiletService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.util.Metrikk
import org.apache.activemq.command.ActiveMQTextMessage
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class InntektsmeldingConsumerTest {

    val ARKIV_ID = "arkivId-123"
    val AR = "NO_ERRORS"
    val AR_MISSING = "UKJENT"
    val AR_YESTERDAY = "AR-666"
    val AR_EIGHT_DAYS = "AR-777"
    val AR_TWO_WEEKS = "AR-666"
    val AR_EXCEPTION = "AR-EXCEPTION"
    val AR_AKTØR_EXCEPTION = "AR-EXCEPTION"

    val MAX_DAYS = 7L

    val NOW = LocalDateTime.now()

    val days1 = FeiletEntitet(arkivReferanse = AR_YESTERDAY, feiltype = Feiltype.AKTØR_FEIL, tidspunkt = NOW.minusDays(1))
    val days14 = FeiletEntitet(arkivReferanse = AR_TWO_WEEKS, feiltype = Feiltype.AKTØR_FEIL, tidspunkt = NOW.minusDays(MAX_DAYS + 7))

    val im = Inntektsmelding(
        arbeidsgiverOrgnummer = "orgnummer",
        arbeidsgiverPrivatFnr = null,
        arbeidsforholdId = "",
        fnr = "fnr",
        journalpostId = "arkivId",
        journalStatus = JournalStatus.MIDLERTIDIG,
        arbeidsgiverperioder = emptyList(),
        arkivRefereranse = "AR-123",
        mottattDato = NOW,
        arsakTilInnsending = "",
        førsteFraværsdag = LocalDate.now()
    )

    @MockK
    private lateinit var metrikk: Metrikk

    @MockK
    private var inntektsmeldingBehandler = mockk<InntektsmeldingBehandler> {
        every { behandle(any(), eq(AR)) } returns ""
        every { behandle(any(), eq(AR_YESTERDAY)) } returns ""
        every { behandle(any(), eq(AR_MISSING)) } returns ""
        every { behandle(any(), eq(AR_TWO_WEEKS)) } returns ""
        every { behandle(any(), eq(AR_AKTØR_EXCEPTION)) } throws FantIkkeAktørException()
    }

    @InjectMockKs(overrideValues = true)
    private lateinit var inntektsmeldingConsumer: InntektsmeldingConsumer

    @MockK
    private var feiletService =  mockk<FeiletService> {
        every {finnHistorikk(AR)} returns Historikk(AR, NOW, emptyList())
        every {finnHistorikk(AR_YESTERDAY)} returns Historikk(AR_YESTERDAY, NOW, listOf(days1))
        every {finnHistorikk(AR_MISSING)} returns Historikk(AR_MISSING, NOW, emptyList())
        every {finnHistorikk(AR_TWO_WEEKS)} returns Historikk(AR_TWO_WEEKS, NOW, listOf(days14))
        every {finnHistorikk(AR_EXCEPTION)} returns Historikk(AR_EXCEPTION, NOW, emptyList())
    }

    @MockK
    private var behandlendeEnhetConsumer = mockk<BehandlendeEnhetConsumer> {
        every { hentBehandlendeEnhet(any()) } returns im.fnr
    }

    @MockK
    private var journalpostService = mockk<JournalpostService> {
        every { hentInntektsmelding(any()) } returns im
    }

    @MockK
    private var oppgaveClient = mockk<OppgaveClient> {
        coEvery { opprettFordelingsOppgave(any(), any(), any()) } returns OppgaveResultat( 1, true )
    }

    companion object {
        private val inputPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "  <ns5:forsendelsesinformasjon xmlns:ns5=\"http://nav.no/melding/virksomhet/dokumentnotifikasjon/v1\" " +
            "    xmlns:ns2=\"http://nav.no/melding/virksomhet/dokumentforsendelse/v1\" " +
            "    xmlns:ns4=\"http://nav.no/dokmot/jms/reply\" " +
            "    xmlns:ns3=\"http://nav.no.dokmot/jms/viderebehandling\">" +
            "  <arkivId>arkivId-123</arkivId>" +
            "  <arkivsystem>JOARK</arkivsystem>" +
            "  <tema>SYK</tema>" +
            "  <behandlingstema>ab0061</behandlingstema>" +
            "</ns5:forsendelsesinformasjon>"
    }

    @Before
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

    @Test
    fun `behandle inntektsmeldingen dersom alle data er gyldig`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR
        inntektsmeldingConsumer.listen(message)
        verify (exactly = 1) { inntektsmeldingBehandler.behandle(eq(ARKIV_ID), AR) }
        verify (exactly = 0) { feiletService.lagreFeilet(any(), any()) }
        coVerify (exactly = 0) { oppgaveClient.opprettFordelingsOppgave(any(), any(), any()) }
        verify (exactly = 0) { metrikk.tellOpprettFordelingsoppgave() }
    }

    @Test
    fun `ignorere inntektsmelding som ikke har arkivReferanse`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        inntektsmeldingConsumer.listen(message)
        verify (exactly = 0) { inntektsmeldingBehandler.behandle(any(), any()) }
        verify (exactly = 0) { feiletService.lagreFeilet(any(), any()) }
        coVerify (exactly = 0) { oppgaveClient.opprettFordelingsOppgave(any(), any(), any()) }
        verify (exactly = 0) { metrikk.tellOpprettFordelingsoppgave() }
    }

    @Test
    fun `opprett fordelingsoppgave dersom inntektsmeldingen har ligget for lenge`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR_TWO_WEEKS
        inntektsmeldingConsumer.listen(message)
        verify (exactly = 0) { inntektsmeldingBehandler.behandle(any(), any()) }
        verify (exactly = 0) { feiletService.lagreFeilet(any(), any()) }
        coVerify (exactly = 1) { oppgaveClient.opprettFordelingsOppgave(eq(ARKIV_ID), any(), any()) }
        verify (exactly = 1) { metrikk.tellOpprettFordelingsoppgave() }
    }

    @Test
    fun `lagre feil dersom det oppstår behandlingsfeil`() {
        val message = ActiveMQTextMessage()
        message.text = inputPayload
        message.jmsCorrelationID = AR_AKTØR_EXCEPTION
        try {
            inntektsmeldingConsumer.listen(message)
        } catch (ex: Exception) {
            verify (exactly = 1) {
                feiletService.lagreFeilet(eq(AR_AKTØR_EXCEPTION), eq(Feiltype.AKTØR_IKKE_FUNNET))
            }
        }
        coVerify (exactly = 0) { oppgaveClient.opprettFordelingsOppgave(any(), any(), any()) }
        verify (exactly = 0) { metrikk.tellOpprettFordelingsoppgave() }
    }

}
