package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.InntektsmeldingRepository
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
    private var om: ObjectMapper = mockk()

    @BeforeEach
    fun setup() {
        oppgaveService = spyk(UtsattOppgaveService(utsattOppgaveDAO, oppgaveClient, behandlendeEnhetConsumer, inntektsmeldingRepository, om, metrikk))
    }

    private val fnr = "fnr"
    private val saksId = "saksId"
    private val aktørId = "aktørId"
    private val journalpostId = "journalpostId"
    private val arkivreferanse = "123"

    @Test
    fun `oppretter forsinket oppgave med timeout`() {
        val timeout = LocalDateTime.of(2020, 4, 6, 9, 0)
        val oppgave = enOppgave(timeout)
        oppgaveService.opprett(oppgave)
        verify { utsattOppgaveDAO.opprett(oppgave) }
    }

    @Test
    fun `lagrerer utsatt oppgave med gjelder speil flagg for OpprettSpeilRelatert`() {
        val timeout = LocalDateTime.of(2023, 4, 6, 9, 0)
        val oppgave = enOppgave(timeout)
        every { utsattOppgaveDAO.finn(any()) } returns oppgave
        every { oppgaveService.opprettOppgave(any(), any(), any()) } returns OppgaveResultat(Random.nextInt(), false, false)
        val oppgaveOppdatering = OppgaveOppdatering(
            UUID.randomUUID(),
            OppdateringstypeDTO.OpprettSpeilRelatert.tilHandling(),
            timeout.plusDays(7),
            OppdateringstypeDTO.OpprettSpeilRelatert
        )
        oppgaveService.prosesser(oppgaveOppdatering)
        verify { utsattOppgaveDAO.lagre(eq(oppgave.copy(tilstand = Tilstand.Opprettet, speil = true))) }
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
