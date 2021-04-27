package no.nav.syfo.behandling

import any
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.producer.InntektsmeldingProducer
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
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

@RunWith(MockitoJUnitRunner::class)
class InntektsmeldingBehandlerTest {

    @Mock
    private val metrikk: Metrikk? = null

    @Mock
    private lateinit var journalpostService: JournalpostService

    @Mock
    private lateinit var utsattOppgaveService: UtsattOppgaveService

    @Mock
    private lateinit var saksbehandlingService: SaksbehandlingService

    @Mock
    private lateinit var aktorConsumer: AktorConsumer

    @Mock
    private lateinit var inntektsmeldingService: InntektsmeldingService

    @Mock
    private val inntektsmeldingProducer: InntektsmeldingProducer? = null

    @Mock
    private val inntektsmeldingAivenProducer: InntektsmeldingAivenProducer? = null

    @InjectMocks
    private lateinit var inntektsmeldingBehandler: InntektsmeldingBehandler

    @Before
    fun setup() {
        `when`(aktorConsumer.getAktorId("fnr")).thenReturn("aktorId") // inntektsmelding.fnr
        `when`(saksbehandlingService.behandleInntektsmelding(any(), matches("aktorId"), matches("AR-123"))).thenReturn("saksId")
        `when`(inntektsmeldingService.lagreBehandling(any(), anyString(), anyString(), anyString())).thenReturn(
                InntektsmeldingEntitet(
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
    fun behandler_midlertidig() {
        `when`(journalpostService.hentInntektsmelding("arkivId", "AR-123")).thenReturn(
            Inntektsmelding(
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                arbeidsgiverPrivatFnr = null,
                arbeidsforholdId = "",
                journalpostId = "arkivId",
                arsakTilInnsending = "",
                journalStatus = JournalStatus.MIDLERTIDIG,
                arbeidsgiverperioder = emptyList(),
                arkivRefereranse = "AR-123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay()
            )
        )
        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        verify(saksbehandlingService).behandleInntektsmelding(any(), anyString(), anyString())
        verify(journalpostService).ferdigstillJournalpost(matches("saksId"), any())
        verify(inntektsmeldingProducer!!).leggMottattInntektsmeldingPåTopics(any())
        verify(inntektsmeldingAivenProducer!!).leggMottattInntektsmeldingPåTopics(any())
    }

    @Test
    fun behandler_Ikke_ForskjelligFraMidlertidig() {
        `when`(journalpostService.hentInntektsmelding("arkivId", "AR-123")).thenReturn(
            Inntektsmelding(
                fnr = "fnr",
                arbeidsforholdId = "123",
                journalpostId = "arkivId",
                arsakTilInnsending = "",
                journalStatus = JournalStatus.ANNET,
                arbeidsgiverperioder = emptyList(),
                arkivRefereranse = "AR-123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay()
            )
        )

        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        verify<SaksbehandlingService>(saksbehandlingService, never()).behandleInntektsmelding(any(), anyString(), anyString())
        verify(journalpostService, never()).ferdigstillJournalpost(any(), any())
    }

    @Test
    fun behandler_Ikke_StatusEndelig() {
        `when`(journalpostService.hentInntektsmelding("arkivId", "AR-123")).thenReturn(
            Inntektsmelding(
                fnr = "fnr",
                arbeidsforholdId = "123",
                journalpostId = "arkivId",
                arsakTilInnsending = "",
                journalStatus = JournalStatus.ENDELIG,
                arbeidsgiverperioder = emptyList(),
                arkivRefereranse = "AR-123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay()
            )
        )

        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        verify<SaksbehandlingService>(saksbehandlingService, never()).behandleInntektsmelding(any(), anyString(), anyString())
        verify(journalpostService, never()).ferdigstillJournalpost(any(), any())
        verify(inntektsmeldingProducer!!, never()).leggMottattInntektsmeldingPåTopics(any())
    }
}

