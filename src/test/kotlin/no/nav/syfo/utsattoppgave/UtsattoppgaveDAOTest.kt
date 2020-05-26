package no.nav.syfo.utsattoppgave

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime.now
import java.util.UUID

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = [(AutoConfigureTestDatabase::class)])
open class UtsattOppgaveDAOTest {

    @Autowired
    private lateinit var repository: UtsattOppgaveRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var utsattOppgaveDAO: UtsattOppgaveDAO

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            System.setProperty("SECURITYTOKENSERVICE_URL", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", "joda")
        }
    }

    @Before
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
        assertNull(maybeOppgave)
    }

    fun oppgave() = UtsattOppgaveEntitet(
        fnr = "fnr",
        sakId = "saksId",
        aktørId = "aktørId",
        journalpostId = "journalpostId",
        arkivreferanse = "arkivreferanse",
        timeout = now(),
        inntektsmeldingId = UUID.randomUUID().toString(),
        tilstand = Tilstand.Utsatt
    )
}