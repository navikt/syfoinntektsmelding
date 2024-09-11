package no.nav.syfo.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.producer.InntektsmeldingAivenProducer
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.util.Metrikk
import no.nav.syfo.util.getAktørid
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class InntektsmeldingBehandlerTest {

    private val metrikk = mockk<Metrikk>(relaxed = true)
    private var journalpostService = mockk<JournalpostService>(relaxed = true)
    private var utsattOppgaveService = mockk<UtsattOppgaveService>(relaxed = true)
    private var inntektsmeldingService = mockk<InntektsmeldingService>(relaxed = true)
    private val aivenInntektsmeldingProducer = mockk<InntektsmeldingAivenProducer>(relaxed = true)
    private val pdlClient = mockk<PdlClient>(relaxed = true)
    private var inntektsmeldingBehandler = InntektsmeldingBehandler(
        journalpostService,
        metrikk,
        inntektsmeldingService,
        aivenInntektsmeldingProducer,
        utsattOppgaveService,
        pdlClient
    )

    @BeforeEach
    fun setup() {
        every { pdlClient.getAktørid("fnr") } returns "aktorId" // inntektsmelding.fnr
        every { inntektsmeldingService.lagreBehandling(any(), any()) } returns
            InntektsmeldingEntitet(
                orgnummer = "orgnummer",
                arbeidsgiverPrivat = "123",
                aktorId = "aktorId",
                journalpostId = "arkivId",
                behandlet = LocalDateTime.now()
            )
    }

    @Test
    fun `Skal feilregistrere duplikater`() {
        // Rigg
        every { journalpostService.hentInntektsmelding("arkivId", "AR-123") } returns
            Inntektsmelding(
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                arbeidsgiverPrivatFnr = null,
                arbeidsforholdId = "",
                journalpostId = "arkivId",
                arsakTilInnsending = "",
                journalStatus = JournalStatus.MOTTATT,
                arbeidsgiverperioder = emptyList(),
                arkivRefereranse = "AR-123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay()
            )
        every { inntektsmeldingService.isDuplicate(any()) } returns true
        // Kjør
        inntektsmeldingBehandler.behandle("arkivId", "AR-123")
        // Verifiser
        verify(exactly = 1) { journalpostService.feilregistrerJournalpost(any()) }
        verify(exactly = 0) { journalpostService.ferdigstillJournalpost(any()) } // not
        verify(exactly = 0) { aivenInntektsmeldingProducer.leggMottattInntektsmeldingPåTopics(any()) }
    }

    @Test
    fun `Skal behandle midlertidig - ikke duplikat`() {
        // Rigg
        every { journalpostService.hentInntektsmelding("arkivId", "AR-123") } returns
            Inntektsmelding(
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                arbeidsgiverPrivatFnr = null,
                arbeidsforholdId = "",
                journalpostId = "arkivId",
                arsakTilInnsending = "",
                journalStatus = JournalStatus.MOTTATT,
                arbeidsgiverperioder = emptyList(),
                arkivRefereranse = "AR-123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay()
            )
        every { inntektsmeldingService.isDuplicate(any()) } returns false
        // Kjør
        inntektsmeldingBehandler.behandle("arkivId", "AR-123")
        // Verifiser
        verify(exactly = 0) { journalpostService.feilregistrerJournalpost(any()) }
        verify(exactly = 1) { journalpostService.ferdigstillJournalpost(any()) }
        verify(exactly = 1) { aivenInntektsmeldingProducer.leggMottattInntektsmeldingPåTopics(any()) }
    }

    @Test
    fun `Skal ignorere alt unntatt midlertidig`() {
        // Rigg
        every { journalpostService.hentInntektsmelding("arkivId", "AR-123") } returns
            Inntektsmelding(
                fnr = "fnr",
                arbeidsforholdId = "123",
                journalpostId = "arkivId",
                arsakTilInnsending = "",
                journalStatus = JournalStatus.UKJENT,
                arbeidsgiverperioder = emptyList(),
                arkivRefereranse = "AR-123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay()
            )
        every { inntektsmeldingService.isDuplicate(any()) } returns false
        // Kjør
        inntektsmeldingBehandler.behandle("arkivId", "AR-123")
        // Verifiser
        verify(exactly = 0) { journalpostService.feilregistrerJournalpost(any()) }
        verify(exactly = 0) { journalpostService.ferdigstillJournalpost(any()) }
        verify(exactly = 0) { aivenInntektsmeldingProducer.leggMottattInntektsmeldingPåTopics(any()) }
    }

    @Test
    fun `Skal ikke behandle ferdigstilt`() {
        // Rigg
        every { journalpostService.hentInntektsmelding("arkivId", "AR-123") } returns
            Inntektsmelding(
                fnr = "fnr",
                arbeidsforholdId = "123",
                journalpostId = "arkivId",
                arsakTilInnsending = "",
                journalStatus = JournalStatus.FERDIGSTILT,
                arbeidsgiverperioder = emptyList(),
                arkivRefereranse = "AR-123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay()
            )
        every { inntektsmeldingService.isDuplicate(any()) } returns false
        // Kjør
        inntektsmeldingBehandler.behandle("arkivId", "AR-123")
        // Verifiser
        verify(exactly = 0) { journalpostService.feilregistrerJournalpost(any()) }
        verify(exactly = 0) { journalpostService.ferdigstillJournalpost(any()) } // not
        verify(exactly = 0) { aivenInntektsmeldingProducer.leggMottattInntektsmeldingPåTopics(any()) }
    }
}
