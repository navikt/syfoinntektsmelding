package no.nav.syfo.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.client.aktor.AktorClient
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.repository.InntektsmeldingService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.service.SaksbehandlingService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class InntektsmeldingBehandlerTest {

    private val metrikk = mockk<Metrikk>(relaxed = true)
    private var journalpostService = mockk<JournalpostService>(relaxed = true)
    private var utsattOppgaveService = mockk<UtsattOppgaveService>(relaxed = true)
    private var saksbehandlingService = mockk<SaksbehandlingService>(relaxed = true)
    private var aktorClient = mockk<AktorClient>(relaxed = true)
    private var inntektsmeldingService = mockk<InntektsmeldingService>(relaxed = true)
    private val aivenInntektsmeldingProducer = mockk<InntektsmeldingAivenProducer>(relaxed = true)

    private var inntektsmeldingBehandler = InntektsmeldingBehandler(
        journalpostService,
        saksbehandlingService,
        metrikk,
        inntektsmeldingService,
        aktorClient,
        aivenInntektsmeldingProducer,
        utsattOppgaveService
    )

    @BeforeEach
    fun setup() {
        every { aktorClient.getAktorId("fnr") } returns "aktorId" // inntektsmelding.fnr
        every {
            saksbehandlingService.behandleInntektsmelding(any(),
                match { it.contentEquals("aktorId") },
                match { it.contentEquals("AR-123") })
        } returns "saksId"
        every { inntektsmeldingService.lagreBehandling(any(), any(), any(), any()) } returns
            InntektsmeldingEntitet(
                orgnummer = "orgnummer",
                arbeidsgiverPrivat = "123",
                aktorId = "aktorId",
                journalpostId = "arkivId",
                sakId = "saksId",
                behandlet = LocalDateTime.now()
            )

    }

    @Test
    fun behandler_midlertidig() {
        every { journalpostService.hentInntektsmelding("arkivId", "AR-123") } returns
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

        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        verify { saksbehandlingService.behandleInntektsmelding(any(), any(), any()) }
        verify { journalpostService.ferdigstillJournalpost(match { it.contentEquals("saksId") }, any()) }
        verify { aivenInntektsmeldingProducer.leggMottattInntektsmeldingPåTopics(any()) }
    }

    @Test
    fun behandler_Ikke_ForskjelligFraMidlertidig() {
        every { journalpostService.hentInntektsmelding("arkivId", "AR-123") } returns
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

        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        verify(exactly = 0) { saksbehandlingService.behandleInntektsmelding(any(), any(), any()) }
        verify(exactly = 0) { journalpostService.ferdigstillJournalpost(any(), any()) }
    }

    @Test
    fun behandler_Ikke_StatusEndelig() {
        every { journalpostService.hentInntektsmelding("arkivId", "AR-123") } returns
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

        inntektsmeldingBehandler.behandle("arkivId", "AR-123")

        verify(exactly = 0) { saksbehandlingService.behandleInntektsmelding(any(), any(), any()) }
        verify(exactly = 0) { journalpostService.ferdigstillJournalpost(any(), any()) }
    }
}

