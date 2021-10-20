package no.nav.syfo.utsattoppgave

import io.ktor.util.*
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.service.BehandlendeEnhetConsumer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*


@KtorExperimentalAPI
open class UtsattOppgaveServiceTest {

    private var utsattOppgaveDAO: UtsattOppgaveDAO = mockk(relaxed = true)
    private var oppgaveClient: OppgaveClient = mockk()
    private var behandlendeEnhetConsumer: BehandlendeEnhetConsumer = mockk()
    private lateinit var oppgaveService: UtsattOppgaveService


    @BeforeEach
    fun setup() {
        oppgaveService = UtsattOppgaveService(utsattOppgaveDAO, oppgaveClient, behandlendeEnhetConsumer)
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

    private fun enOppgave(timeout: LocalDateTime, tilstand: Tilstand = Tilstand.Utsatt) = UtsattOppgaveEntitet(
        fnr = fnr,
        sakId = saksId,
        aktørId = aktørId,
        journalpostId = journalpostId,
        arkivreferanse = arkivreferanse,
        timeout = timeout,
        inntektsmeldingId = UUID.randomUUID().toString(),
        tilstand = tilstand,
        gosysOppgaveId = null,
        oppdatert = null
    )

}

