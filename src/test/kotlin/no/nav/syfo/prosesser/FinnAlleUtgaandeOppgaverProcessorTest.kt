package no.nav.syfo.prosesser

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.syfo.UtsattOppgaveTestData
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.koin.buildObjectMapper
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.random.Random

class FinnAlleUtgaandeOppgaverProcessorTest {
    private val utsattOppgaveDAO: UtsattOppgaveDAO = mockk(relaxed = true)
    private val oppgaveClient: OppgaveClient = mockk(relaxed = true)
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer = mockk(relaxed = true)
    private val metrikk: Metrikk = mockk(relaxed = true)
    private val inntektsmeldingRepository: InntektsmeldingRepository = mockk(relaxed = true)
    private val om = buildObjectMapper()
    private lateinit var processor: FinnAlleUtgaandeOppgaverProcessor

    private val oppgave = UtsattOppgaveTestData.oppgave.copy()
    private val timeout = LocalDateTime.of(2023, 4, 6, 9, 0)

    @BeforeEach
    fun setup() {
        processor = spyk(
            FinnAlleUtgaandeOppgaverProcessor(
                utsattOppgaveDAO,
                oppgaveClient,
                behandlendeEnhetConsumer,
                metrikk,
                inntektsmeldingRepository,
                om
            )
        )
        every { utsattOppgaveDAO.finnAlleUtgåtteOppgaver() } returns listOf(oppgave.copy())
        coEvery { oppgaveClient.opprettOppgave(any(), any(), any()) } returns OppgaveResultat(Random.nextInt(), false, false)
        every { inntektsmeldingRepository.findByUuid(any()) } returns UtsattOppgaveTestData.inntektsmeldingEntitet
        every { behandlendeEnhetConsumer.hentBehandlendeEnhet(any(), any()) } returns "4488"
    }

    @Test
    fun `Oppretter oppgave ved timout og lagrer tilstand OpprettetTimeout`() {
        processor.doJob()
        verify { utsattOppgaveDAO.lagre(match { it.tilstand == Tilstand.OpprettetTimeout && !it.speil && it.timeout == timeout && it.oppdatert != oppgave.oppdatert }) }
        coVerify { oppgaveClient.opprettOppgave(any(), any(), any()) }
    }

    @Test
    fun `Oppretter ikke oppgave ved timeout hvis begrunnelseRedusert = IkkeFravaer`() {
        every { inntektsmeldingRepository.findByUuid(any()) } returns UtsattOppgaveTestData.inntektsmeldingEntitetIkkeFravaer
        processor.doJob()
        verify { utsattOppgaveDAO.lagre(match { it.tilstand == Tilstand.Forkastet && !it.speil && it.timeout == timeout && it.oppdatert != oppgave.oppdatert }) }
        coVerify(exactly = 0) { oppgaveClient.opprettOppgave(any(), any(), any()) }
    }
}
