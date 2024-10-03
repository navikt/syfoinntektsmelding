package no.nav.syfo.client

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.just
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.oppgave.OppgaveClient
import no.nav.helsearbeidsgiver.oppgave.domain.Behandlingstema
import no.nav.helsearbeidsgiver.oppgave.domain.Behandlingstype
import no.nav.helsearbeidsgiver.oppgave.domain.Oppgave
import no.nav.helsearbeidsgiver.oppgave.domain.OppgaveListeResponse
import no.nav.helsearbeidsgiver.oppgave.domain.Oppgavetype
import no.nav.helsearbeidsgiver.oppgave.domain.OpprettOppgaveRequest
import no.nav.helsearbeidsgiver.oppgave.domain.OpprettOppgaveResponse
import no.nav.helsearbeidsgiver.oppgave.domain.Prioritet
import no.nav.helsearbeidsgiver.oppgave.exception.OpprettOppgaveFeiletException
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.domain.OppgaveResultat
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.BehandlingsKategori
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class OppgaveServiceTest {
    @MockK
    private lateinit var oppgaveClient: OppgaveClient

    @MockK
    private lateinit var metrikk: Metrikk

    private lateinit var oppgaveService: OppgaveService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        oppgaveService = OppgaveService(oppgaveClient, metrikk)
    }

    @Test
    fun `skal opprette fordelingsoppgave uten feil`() =
        runBlocking {
            val journalpostId = "123"

            coEvery { oppgaveClient.hentOppgaver(any()) } returns OppgaveListeResponse(0, emptyList())
            coEvery { oppgaveClient.opprettOppgave(any<OpprettOppgaveRequest>()) } returns OpprettOppgaveResponse(1)

            val result = oppgaveService.opprettFordelingsOppgave(journalpostId)

            coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstype == null }) }
            coVerify { oppgaveClient.opprettOppgave(match { it.journalpostId == journalpostId }) }
            assertNotNull(result)
            assertEquals(1, result.oppgaveId)
            assertFalse(result.utbetalingBruker)
            assertFalse(result.duplikat)
        }

    @Test
    fun `skal returnere eksistrende fordelingsoppgave dersom det finnes fra før`() =
        runBlocking {
            val journalpostId = "123"

            coEvery { oppgaveClient.hentOppgaver(any()) } returns
                OppgaveListeResponse(
                    1,
                    listOf(Oppgave(id = 1, oppgavetype = Oppgavetype.INNTEKTSMELDING, prioritet = Prioritet.NORM.name, aktivDato = LocalDate.now())),
                )
            val result = oppgaveService.opprettFordelingsOppgave(journalpostId)
            coVerify(exactly = 0) {
                oppgaveClient.opprettOppgave(any<OpprettOppgaveRequest>())
            }
            assertNotNull(result)
            assertEquals(1, result.oppgaveId)
            assertFalse(result.utbetalingBruker)
            assertTrue(result.duplikat)
        }

    @Test
    fun `skal ikke opprette oppgave dersom oppgave finnes fra før`() =
        runBlocking {
            val behandlingsKategori = BehandlingsKategori.UTLAND
            val journalpostId = "123"
            val aktoerId = "456"

            val expectedOppgaveResultat = OppgaveResultat(oppgaveId = 1, false, false)
            coEvery { oppgaveClient.opprettOppgave(any()) } returns OpprettOppgaveResponse(1)
            coEvery { metrikk.tellOpprettOppgave(any()) } just Runs
            coEvery { oppgaveClient.hentOppgaver(any()) } returns
                OppgaveListeResponse(
                    1,
                    listOf(Oppgave(id = 1, oppgavetype = Oppgavetype.INNTEKTSMELDING, prioritet = Prioritet.NORM.name, aktivDato = LocalDate.now())),
                )
            val result = oppgaveService.opprettOppgave(journalpostId, aktoerId, behandlingsKategori)
            coVerify(exactly = 0) {
                oppgaveClient.opprettOppgave(any<OpprettOppgaveRequest>())
            }

            assertNotNull(result)
            assertEquals(expectedOppgaveResultat.oppgaveId, result.oppgaveId)
            assertEquals(expectedOppgaveResultat.utbetalingBruker, result.utbetalingBruker)
        }

    @Test
    fun `skal kaste feil når oppretteOppgave  feiler`() =
        runBlocking {
            val journalpostId = "123"
            val aktoerId = "456"
            val behandlingsKategori = BehandlingsKategori.REFUSJON_MED_DATO
            coEvery { oppgaveClient.hentOppgaver(any()) } returns OppgaveListeResponse(0, emptyList())
            coEvery { oppgaveClient.opprettOppgave(any()) } throws OpprettOppgaveFeiletException(Exception("Feil ved oppretting av oppgave"))
            coEvery { metrikk.tellOpprettOppgave(any()) } just Runs

            val exception =
                assertThrows<OpprettOppgaveException> {
                    oppgaveService.opprettOppgave(journalpostId, aktoerId, behandlingsKategori)
                }

            assertNotNull(exception)
        }

    @Test
    fun `Skal opprette oppgave med riktig behandlingstema og behandlingsType og utbetaling til bruker SPEIL`() =
        runBlocking {
            val behandlingsKategori = BehandlingsKategori.SPEIL_RELATERT
            val result = mockAndRunOpprettOppgave(behandlingsKategori)
            assertNotNull(result)
            assertEquals(1, result.oppgaveId)
            assertEquals(false, result.utbetalingBruker)
        }

    @Test
    fun `Skal opprette oppgave med riktig behandlingstema og behandlingsType og utbetaling til bruker  BESTRIDER_SYKEMELDING`() =
        runBlocking {
            val behandlingsKategori = BehandlingsKategori.BESTRIDER_SYKEMELDING

            val result = mockAndRunOpprettOppgave(behandlingsKategori)
            coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstema == Behandlingstema.BESTRIDER_SYKEMELDING }) }
            coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstype == null }) }

            assertNotNull(result)
            assertEquals(1, result.oppgaveId)
            assertFalse(result.utbetalingBruker)
        }

    @Test
    fun `Skal opprette oppgave med riktig behandlingstema og behandlingsType og utbetaling til bruker UTLAND`() =
        runBlocking {
            val behandlingsKategori = BehandlingsKategori.UTLAND

            val result = mockAndRunOpprettOppgave(behandlingsKategori)

            coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstema == Behandlingstema.NORMAL }) }
            coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstype == Behandlingstype.UTLAND }) }

            assertNotNull(result)
            assertEquals(1, result.oppgaveId)
            assertEquals(false, result.utbetalingBruker)
        }

    @Test
    fun `Skal opprette oppgave med riktig behandlingstema og behandlingsType og utbetaling til bruker REFUSJON_UTEN_DATO`() =
        runBlocking {
            val behandlingsKategori = BehandlingsKategori.REFUSJON_UTEN_DATO

            val result = mockAndRunOpprettOppgave(behandlingsKategori)

            coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstema == Behandlingstema.NORMAL }) }
            coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstype == null }) }

            assertNotNull(result)
            assertEquals(1, result.oppgaveId)
            assertEquals(false, result.utbetalingBruker)
        }

    @Test
    fun `Skal opprette oppgave med riktig behandlingstema og behandlingsType og utbetaling til bruker IKKE_FRAVAER`() =
        runBlocking {
            val behandlingsKategori = BehandlingsKategori.REFUSJON_UTEN_DATO

            val result = mockAndRunOpprettOppgave(behandlingsKategori)

            coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstema == Behandlingstema.NORMAL }) }
            coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstype == null }) }

            assertNotNull(result)
            assertEquals(1, result.oppgaveId)
            assertEquals(false, result.utbetalingBruker)
        }

    @Test
    fun `Skal opprette oppgave med riktig behandlingstema og behandlingsType og utbetaling til bruker IKKE_REFUSJON `() =
        runBlocking {
            val behandlingsKategori = BehandlingsKategori.IKKE_REFUSJON
            assertBehandlingsTemaUtbetalingTilBruker(behandlingsKategori)
        }

    @Test
    fun `Skal opprette oppgave med riktig behandlingstema og behandlingsType og utbetaling til bruker REFUSJON_MED_DATO `() =
        runBlocking {
            val behandlingsKategori = BehandlingsKategori.REFUSJON_MED_DATO
            assertBehandlingsTemaUtbetalingTilBruker(behandlingsKategori)
        }

    @Test
    fun `Skal opprette oppgave med riktig behandlingstema og behandlingsType og utbetaling til bruker REFUSJON_LITEN_LØNN `() =
        runBlocking {
            val behandlingsKategori = BehandlingsKategori.REFUSJON_LITEN_LØNN
            assertBehandlingsTemaUtbetalingTilBruker(behandlingsKategori)
        }

    private suspend fun OppgaveServiceTest.assertBehandlingsTemaUtbetalingTilBruker(behandlingsKategori: BehandlingsKategori) {
        val result = mockAndRunOpprettOppgave(behandlingsKategori)

        coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstema == Behandlingstema.UTBETALING_TIL_BRUKER }) }
        coVerify { oppgaveClient.opprettOppgave(match { it.behandlingstype == null }) }

        assertNotNull(result)
        assertEquals(1, result.oppgaveId)
        assertTrue(result.utbetalingBruker)
    }

    private suspend fun mockAndRunOpprettOppgave(behandlingsKategori: BehandlingsKategori): OppgaveResultat {
        val journalpostId = "123"
        val aktoerId = "456"

        coEvery { oppgaveClient.hentOppgaver(any()) } returns OppgaveListeResponse(0, emptyList())
        coEvery { metrikk.tellOpprettOppgave(any()) } just Runs
        coEvery { oppgaveClient.opprettOppgave(any<OpprettOppgaveRequest>()) } returns OpprettOppgaveResponse(1)
        val result = oppgaveService.opprettOppgave(journalpostId, aktoerId, behandlingsKategori)
        return result
    }
}
