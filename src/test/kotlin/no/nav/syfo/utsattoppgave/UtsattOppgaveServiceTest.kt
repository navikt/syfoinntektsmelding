package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
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

    private var utsattOppgaveDAO: UtsattOppgaveDAO = mockk(relaxed = true)
    private var oppgaveClient: OppgaveClient = mockk(relaxed = true)
    private var behandlendeEnhetConsumer: BehandlendeEnhetConsumer = mockk()
    private lateinit var oppgaveService: UtsattOppgaveService
    private var metrikk: Metrikk = mockk(relaxed = true)
    private var inntektsmeldingRepository: InntektsmeldingRepository = mockk(relaxed = true)
    private var om: ObjectMapper =
        ObjectMapper().registerModules(
            KotlinModule
                .Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build(),
            JavaTimeModule(),
        )

    @BeforeEach
    fun setup() {
        oppgaveService = spyk(UtsattOppgaveService(utsattOppgaveDAO, oppgaveClient, behandlendeEnhetConsumer, inntektsmeldingRepository, om, metrikk))
        every { utsattOppgaveDAO.finn(any()) } returns oppgave
        coEvery { oppgaveClient.opprettOppgave(any(), any(), any()) } returns OppgaveResultat(Random.nextInt(), false, false)
        every { inntektsmeldingRepository.findByUuid(any()) } returns
            InntektsmeldingEntitet(
                aktorId = "aktoerid-123",
                behandlet = LocalDateTime.now(),
                orgnummer = "arb-org-123",
                journalpostId = "jp-123",
                data = om.writeValueAsString(buildIM()),
            )
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
        verify { utsattOppgaveDAO.lagre(eq(oppgave.copy(tilstand = Tilstand.Opprettet, speil = true, timeout = timeout))) }
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
        verify { utsattOppgaveDAO.lagre(eq(oppgave.copy(tilstand = Tilstand.Opprettet, speil = false, timeout = timeout))) }
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
        verify { utsattOppgaveDAO.lagre(eq(oppgave.copy(tilstand = Tilstand.Utsatt, timeout = nyTimeout))) }
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
        verify { utsattOppgaveDAO.lagre(eq(oppgave.copy(tilstand = Tilstand.Forkastet, timeout = timeout))) }
        coVerify { oppgaveClient.opprettOppgave(any(), any(), any()) wasNot Called }
    }

    private fun enOppgave(
        timeout: LocalDateTime,
        tilstand: Tilstand = Tilstand.Utsatt,
    ) = UtsattOppgaveEntitet(
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
        utbetalingBruker = false,
    )
}
