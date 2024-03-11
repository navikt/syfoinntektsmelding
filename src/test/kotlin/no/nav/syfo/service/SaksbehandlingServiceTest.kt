package no.nav.syfo.service

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.util.getAktørid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Collections.emptyList

class SaksbehandlingServiceTest {

    private var oppgaveClient = mockk<OppgaveClient>(relaxed = true)
    private var pdlClient = mockk<PdlClient>(relaxed = true)
    private var inntektsmeldingService = mockk<InntektsmeldingService>(relaxed = true)

    @BeforeEach
    fun setup() {
        every { inntektsmeldingService.finnBehandledeInntektsmeldinger(any()) } returns emptyList()
        every { pdlClient.getAktørid(any()) } returns "aktorid"
    }

    @Test
    fun oppretterIkkeOppgaveForSak() {
        runBlocking {
            coVerify(exactly = 0) { oppgaveClient.opprettOppgave(any(), any(), any(), any(), any(), any()) }
        }
    }

    private fun lagInntektsmelding(
        aktorId: String,
        journalpostId: String,
        sakId: String,
        arbeidsgiverperioder: List<Periode>
    ): Inntektsmelding {
        return Inntektsmelding(
            id = "ID",
            fnr = "fnr",
            arbeidsgiverOrgnummer = "orgnummer",
            arbeidsforholdId = null,
            journalpostId = journalpostId,
            arsakTilInnsending = "Ny",
            journalStatus = JournalStatus.MOTTATT,
            arbeidsgiverperioder = arbeidsgiverperioder,
            arkivRefereranse = "AR",
            førsteFraværsdag = LocalDate.now(),
            mottattDato = LocalDate.of(2019, 2, 6).atStartOfDay(),
            sakId = sakId,
            aktorId = aktorId
        )
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmFomOverlapper() {
        every { inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId") } returns
            listOf(
                lagInntektsmelding(
                    aktorId = "aktorId",
                    journalpostId = "journalPostId",
                    sakId = "1",
                    arbeidsgiverperioder = listOf(
                        Periode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 1, 24)
                        )
                    )
                )
            )
    }

    @Test
    fun girNyInntektsmeldingEksisterendeSakIdOmFomOgTomOverlapper() {
        every { inntektsmeldingService.finnBehandledeInntektsmeldinger("aktorId") } returns
            listOf(
                lagInntektsmelding(
                    aktorId = "aktorId",
                    journalpostId = "journalPostId",
                    sakId = "1",
                    arbeidsgiverperioder = listOf(
                        Periode(
                            fom = LocalDate.of(2019, 1, 1),
                            tom = LocalDate.of(2019, 1, 20)
                        )
                    )
                )
            )
    }
}
