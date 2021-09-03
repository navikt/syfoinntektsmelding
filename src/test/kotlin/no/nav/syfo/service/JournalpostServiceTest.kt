package no.nav.syfo.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.service.BehandleInngaaendeJournalConsumer
import no.nav.syfo.service.InngaaendeJournalConsumer
import no.nav.syfo.service.JournalConsumer
import no.nav.syfo.domain.InngaaendeJournal
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.service.JournalpostService
import no.nav.syfo.util.Metrikk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import java.time.LocalDate
import java.time.LocalDateTime

class JournalpostServiceTest {
    private var inngaaendeJournalConsumer = mockk<InngaaendeJournalConsumer>(relaxed = true)
    private var behandlendeEnhetConsumer = mockk<BehandlendeEnhetConsumer>(relaxed = true)
    private var journalConsumer = mockk<JournalConsumer>(relaxed = true)
    private var behandleInngaaendeJournalConsumer = mockk<BehandleInngaaendeJournalConsumer>(relaxed = true)
    private val metrikk = mockk<Metrikk>(relaxed = true)

    private val journalpostService = JournalpostService(
        inngaaendeJournalConsumer,
        behandleInngaaendeJournalConsumer,
        journalConsumer,
        behandlendeEnhetConsumer,
        metrikk
    )

    @Test
    fun ferdigstillJournalpost() {
        val journal = InngaaendeJournal(dokumentId = "dokumentId", status = JournalStatus.MIDLERTIDIG, mottattDato = LocalDateTime.now())
        every { inngaaendeJournalConsumer.hentDokumentId("journalpostId") } returns journal
        every { behandlendeEnhetConsumer.hentBehandlendeEnhet(any(), any()) } returns "enhet"

        journalpostService.ferdigstillJournalpost(
            "saksId",
            Inntektsmelding(
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                arbeidsforholdId = null,
                journalpostId = "journalpostId",
                arsakTilInnsending = "Ny",
                journalStatus = JournalStatus.MIDLERTIDIG,
                arkivRefereranse = "AR123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDateTime.now()
            )
        )

        verify { behandlendeEnhetConsumer.hentBehandlendeEnhet("fnr", "") }
        verify { inngaaendeJournalConsumer.hentDokumentId("journalpostId") }
        verify { behandleInngaaendeJournalConsumer.oppdaterJournalpost(any()) }
        verify { behandleInngaaendeJournalConsumer.ferdigstillJournalpost(any()) }
    }

    @Test
    fun hentInntektsmelding() {
        val journal = InngaaendeJournal(dokumentId = "dokumentId", status = JournalStatus.MIDLERTIDIG, mottattDato = LocalDateTime.now())
        every { inngaaendeJournalConsumer.hentDokumentId("journalpostId") } returns journal
        every { journalConsumer.hentInntektsmelding("journalpostId", "AR-1234") } returns
            Inntektsmelding(
                fnr = "fnr",
                arbeidsgiverOrgnummer = "orgnummer",
                arbeidsgiverPrivatFnr = null,
                journalpostId = "journalpostId",
                arsakTilInnsending = "",
                journalStatus = JournalStatus.MIDLERTIDIG,
                arkivRefereranse = "AR123",
                førsteFraværsdag = LocalDate.now(),
                mottattDato = LocalDateTime.now()
            )


        val (_, fnr, _, arbeidsgiverPrivat) = journalpostService.hentInntektsmelding("journalpostId", "AR-1234")

        assertThat(fnr).isEqualTo("fnr")
        assertThat(arbeidsgiverPrivat).isNull()
    }
}
