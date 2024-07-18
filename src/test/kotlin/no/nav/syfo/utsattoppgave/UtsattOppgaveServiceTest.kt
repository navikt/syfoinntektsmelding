package no.nav.syfo.utsattoppgave

import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.koin.buildObjectMapper
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.buildIM
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.util.Metrikk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

open class UtsattOppgaveServiceTest {

    private val utsattOppgaveDAO: UtsattOppgaveDAO = mockk(relaxed = true)
    private val oppgaveClient: OppgaveClient = mockk(relaxed = true)
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer = mockk(relaxed = true)
    private lateinit var oppgaveService: UtsattOppgaveService
    private val metrikk: Metrikk = mockk(relaxed = true)
    private val inntektsmeldingRepository: InntektsmeldingRepository = mockk(relaxed = true)
    private val om = buildObjectMapper()

    val inntektsmeldingEntitet = InntektsmeldingEntitet(
        aktorId = "aktoerid-123",
        behandlet = LocalDateTime.now(),
        orgnummer = "arb-org-123",
        journalpostId = "jp-123",
        data = om.writeValueAsString(buildIM()),
    )
    val inntektsmeldingEntitetIkkeFravaer = inntektsmeldingEntitet.copy(data = om.writeValueAsString(buildIM().copy(begrunnelseRedusert = "IkkeFravaer")))

    @BeforeEach
    fun setup() {
        oppgaveService = spyk(UtsattOppgaveService(utsattOppgaveDAO, oppgaveClient, behandlendeEnhetConsumer, inntektsmeldingRepository, om, metrikk))
        every { utsattOppgaveDAO.finn(any()) } returns oppgave.copy()
        coEvery { oppgaveClient.opprettOppgave(any(), any(), any()) } returns OppgaveResultat(Random.nextInt(), false, false)
        every { inntektsmeldingRepository.findByUuid(any()) } returns inntektsmeldingEntitet
        every { behandlendeEnhetConsumer.hentBehandlendeEnhet(any(), any()) } returns "4488"
    }

    private val fnr = "fnr"
    private val aktørId = "aktørId"
    private val journalpostId = "journalpostId"
    private val arkivreferanse = "123"

    private val timeout = LocalDateTime.of(2023, 4, 6, 9, 0)
    private val oppgave = enOppgave(timeout)

    @Test
    fun `Oppretter forsinket oppgave med timeout`() {
        oppgaveService.opprett(oppgave)
        verify { utsattOppgaveDAO.opprett(oppgave) }
    }

    @Test
    fun `Lagrer utsatt oppgave med gjelder speil flagg og tilstand Opprettet for OpprettSpeilRelatert`() {
        val oppgaveOppdatering = OppgaveOppdatering(
            UUID.randomUUID(),
            OppdateringstypeDTO.OpprettSpeilRelatert.tilHandling(),
            timeout.plusDays(7),
            OppdateringstypeDTO.OpprettSpeilRelatert
        )
        oppgaveService.prosesser(oppgaveOppdatering)
        verify { utsattOppgaveDAO.lagre(match { it.tilstand == Tilstand.Opprettet && it.speil && it.timeout == timeout }) }
        coVerify { oppgaveClient.opprettOppgave(any(), any(), any()) }
    }

    @Test
    fun `Lagrer utsatt oppgave med tilstand Opprettet for Opprett`() {
        val oppgaveOppdatering = OppgaveOppdatering(
            UUID.randomUUID(),
            OppdateringstypeDTO.Opprett.tilHandling(),
            timeout.plusDays(7),
            OppdateringstypeDTO.Opprett
        )
        oppgaveService.prosesser(oppgaveOppdatering)
        verify { utsattOppgaveDAO.lagre(match { it.tilstand == Tilstand.Opprettet && !it.speil && it.timeout == timeout }) }
        coVerify { oppgaveClient.opprettOppgave(any(), any(), any()) }
    }

    @Test
    fun `Lagrer utsatt oppgave med ny timeout for utsettelse`() {
        val nyTimeout = timeout.plusDays(7)
        val oppgaveOppdatering = OppgaveOppdatering(
            UUID.randomUUID(),
            OppdateringstypeDTO.Utsett.tilHandling(),
            nyTimeout,
            OppdateringstypeDTO.Utsett
        )
        oppgaveService.prosesser(oppgaveOppdatering)
        verify { utsattOppgaveDAO.lagre(match { it.tilstand == Tilstand.Utsatt && it.timeout == nyTimeout && it.oppdatert != oppgave.oppdatert }) }
        coVerify { oppgaveClient.opprettOppgave(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Lagrer utsatt oppgave med tilstand Forkastet ved Ferdigbehandlet`() {
        val oppgaveOppdatering = OppgaveOppdatering(
            UUID.randomUUID(),
            OppdateringstypeDTO.Ferdigbehandlet.tilHandling(),
            timeout.plusDays(7),
            OppdateringstypeDTO.Ferdigbehandlet
        )
        oppgaveService.prosesser(oppgaveOppdatering)
        verify { utsattOppgaveDAO.lagre(match { it.tilstand == Tilstand.Forkastet && it.timeout == timeout && it.oppdatert != oppgave.oppdatert }) }
        coVerify(exactly = 0) { oppgaveClient.opprettOppgave(any(), any(), any()) }
    }

    @Test
    fun `Oppretter Ikke Oppgave hvis begrunnelseRedusert = IkkeFravaer hvis oppgave utsatt`() {
        every { inntektsmeldingRepository.findByUuid(any()) } returns inntektsmeldingEntitetIkkeFravaer
        val oppgaveOppdatering = OppgaveOppdatering(
            UUID.randomUUID(),
            OppdateringstypeDTO.Opprett.tilHandling(),
            timeout.plusDays(7),
            OppdateringstypeDTO.Opprett
        )
        oppgaveService.prosesser(oppgaveOppdatering)
        verify { utsattOppgaveDAO.lagre(match { it.tilstand == Tilstand.Forkastet && it.oppdatert != oppgave.oppdatert }) }
        coVerify(exactly = 0) { oppgaveClient.opprettOppgave(any(), any(), any()) }
    }

    @Test
    fun `Oppretter Ikke Oppgave hvis begrunnelseRedusert = IkkeFravaer og oppgave allerede forkastet`() {
        every { inntektsmeldingRepository.findByUuid(any()) } returns inntektsmeldingEntitetIkkeFravaer
        val forkastetTidspunkt = LocalDateTime.of(2023, 4, 6, 9, 0)
        val forkastetOppgave = oppgave.copy(tilstand = Tilstand.Forkastet, oppdatert = forkastetTidspunkt)
        every { utsattOppgaveDAO.finn(any()) } returns forkastetOppgave
        val oppgaveOppdatering = OppgaveOppdatering(
            UUID.randomUUID(),
            OppdateringstypeDTO.Opprett.tilHandling(),
            timeout.plusDays(7),
            OppdateringstypeDTO.Opprett
        )
        oppgaveService.prosesser(oppgaveOppdatering)
        verify(exactly = 0) { utsattOppgaveDAO.lagre(any()) }
        coVerify(exactly = 0) { oppgaveClient.opprettOppgave(any(), any(), any()) }
    }

    private fun enOppgave(timeout: LocalDateTime, tilstand: Tilstand = Tilstand.Utsatt) = UtsattOppgaveEntitet(
        fnr = fnr,
        aktørId = aktørId,
        journalpostId = journalpostId,
        arkivreferanse = arkivreferanse,
        timeout = timeout,
        inntektsmeldingId = UUID.randomUUID().toString(),
        tilstand = tilstand,
        gosysOppgaveId = null,
        oppdatert = null,
        speil = false,
        utbetalingBruker = false
    )
}
