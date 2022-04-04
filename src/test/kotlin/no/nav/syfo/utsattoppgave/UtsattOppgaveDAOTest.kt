package no.nav.syfo.utsattoppgave

import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository
import no.nav.syfo.repository.UtsattOppgaveRepositoryMockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now
import java.util.UUID

open class UtsattOppgaveDAOTest {

    private var repository: UtsattOppgaveRepository = UtsattOppgaveRepositoryMockk()

    private lateinit var utsattOppgaveDAO: UtsattOppgaveDAO

    @BeforeEach
    fun setup() {
        utsattOppgaveDAO = UtsattOppgaveDAO(repository)
    }

    @Test
    fun `kan opprette oppgave`() {
        val oppgave1 = oppgave()
        utsattOppgaveDAO.opprett(oppgave1)
        val oppgave = utsattOppgaveDAO.finn(oppgave1.inntektsmeldingId)
        assertNotNull(oppgave)
        assertEquals(oppgave1.arkivreferanse, oppgave!!.arkivreferanse)
    }

    @Test
    fun `to fremtidige oppgaver har forskjellig id`() {
        val oppgave1 = oppgave()
        val oppgave2 = oppgave()
        val id1 = utsattOppgaveDAO.opprett(oppgave1)
        val id2 = utsattOppgaveDAO.opprett(oppgave2)

        val _oppgave1 = utsattOppgaveDAO.finn(oppgave1.inntektsmeldingId)!!
        assertEquals(id1, _oppgave1.id)
        val _oppgave2 = utsattOppgaveDAO.finn(oppgave2.inntektsmeldingId)!!
        assertEquals(id2, _oppgave2.id)
    }

    @Test
    fun `kan oppdatere tilstand til en oppgave`() {
        val oppgave1 = oppgave()
        val oppgave2 = oppgave()
        utsattOppgaveDAO.opprett(oppgave1)
        utsattOppgaveDAO.opprett(oppgave2)

        oppgave1.tilstand = Tilstand.Forkastet
        utsattOppgaveDAO.lagre(oppgave1)

        val _oppgave1 = utsattOppgaveDAO.finn(oppgave1.inntektsmeldingId)!!
        assertEquals(Tilstand.Forkastet, _oppgave1.tilstand)
        val _oppgave2 = utsattOppgaveDAO.finn(oppgave2.inntektsmeldingId)!!
        assertEquals(Tilstand.Utsatt, _oppgave2.tilstand)
    }

    @Test
    fun `finner ikke oppgave på manglende inntektsmelding`() {
        val maybeOppgave = utsattOppgaveDAO.finn(UUID.randomUUID().toString())
        Assertions.assertNull(maybeOppgave)
    }

    private fun oppgave() = UtsattOppgaveEntitet(
        fnr = "fnr",
        sakId = "saksId",
        aktørId = "aktørId",
        journalpostId = "journalpostId",
        arkivreferanse = "arkivreferanse",
        timeout = now(),
        inntektsmeldingId = UUID.randomUUID().toString(),
        tilstand = Tilstand.Utsatt,
        gosysOppgaveId = null,
        oppdatert = null,
        speil = false,
        utbetalingBruker = false
    )
}
