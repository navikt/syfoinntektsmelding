package no.nav.syfo.client

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.oppgave.OppgaveClient
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
import org.junit.jupiter.api.Assertions.assertNotNull
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
            val expectedOppgaveResultat = OppgaveResultat(oppgaveId = 1, duplikat = false, utbetalingBruker = false)
            coEvery { oppgaveClient.hentOppgaver(any()) } returns OppgaveListeResponse(0, emptyList())
            coEvery { oppgaveClient.opprettOppgave(any<OpprettOppgaveRequest>()) } returns OpprettOppgaveResponse(1)

            val result = oppgaveService.opprettFordelingsOppgave(journalpostId)

            assertNotNull(result)
            assertEquals(expectedOppgaveResultat.oppgaveId, result.oppgaveId)
            assertEquals(expectedOppgaveResultat.utbetalingBruker, result.utbetalingBruker)
        }

    @Test
    fun `skal returnere oppgave dersom det finnes fra før`() =
        runBlocking {
            val journalpostId = "123"
            val expectedOppgaveResultat = OppgaveResultat(oppgaveId = 1, duplikat = true, utbetalingBruker = false)
            coEvery { oppgaveClient.hentOppgaver(any()) } returns
                OppgaveListeResponse(
                    1,
                    listOf(Oppgave(id = 1, oppgavetype = Oppgavetype.INNTEKTSMELDING, prioritet = Prioritet.NORM.name, aktivDato = LocalDate.now())),
                )
            val result = oppgaveService.opprettFordelingsOppgave(journalpostId)
            verify(exactly = 0) {
                runBlocking { oppgaveClient.opprettOppgave(any<OpprettOppgaveRequest>()) }
            }
            assertNotNull(result)
            assertEquals(expectedOppgaveResultat.oppgaveId, result.oppgaveId)
            assertEquals(expectedOppgaveResultat.utbetalingBruker, result.utbetalingBruker)
        }

    @Test
    fun `skal opprette oppgave uten feil`() =
        runBlocking {
            val journalpostId = "123"
            val aktoerId = "456"
            val behandlingsKategori = BehandlingsKategori.UTLAND
            val expectedOppgaveResultat = OppgaveResultat(oppgaveId = 1, false, false)
            coEvery { oppgaveClient.hentOppgaver(any()) } returns OppgaveListeResponse(0, emptyList())
            coEvery { oppgaveClient.opprettOppgave(any()) } returns OpprettOppgaveResponse(1)
            coEvery { metrikk.tellOpprettOppgave(any()) } just Runs

            val result = oppgaveService.opprettOppgave(journalpostId, aktoerId, behandlingsKategori)

            assertNotNull(result)
            assertEquals(expectedOppgaveResultat.oppgaveId, result.oppgaveId)
            assertEquals(expectedOppgaveResultat.utbetalingBruker, result.utbetalingBruker)
        }

    @Test
    fun `skal ikke opprette oppgave dersom oppgave finnes fra før`() =
        runBlocking {
            val journalpostId = "123"
            val aktoerId = "456"
            val behandlingsKategori = BehandlingsKategori.UTLAND
            val expectedOppgaveResultat = OppgaveResultat(oppgaveId = 1, false, false)
            coEvery { oppgaveClient.opprettOppgave(any()) } returns OpprettOppgaveResponse(1)
            coEvery { metrikk.tellOpprettOppgave(any()) } just Runs
            coEvery { oppgaveClient.hentOppgaver(any()) } returns
                OppgaveListeResponse(
                    1,
                    listOf(Oppgave(id = 1, oppgavetype = Oppgavetype.INNTEKTSMELDING, prioritet = Prioritet.NORM.name, aktivDato = LocalDate.now())),
                )
            val result = oppgaveService.opprettOppgave(journalpostId, aktoerId, behandlingsKategori)
            verify(exactly = 0) {
                runBlocking { oppgaveClient.opprettOppgave(any<OpprettOppgaveRequest>()) }
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
}
