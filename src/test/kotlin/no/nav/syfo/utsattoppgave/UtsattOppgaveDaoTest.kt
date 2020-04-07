package no.nav.syfo.utsattoppgave

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
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
import javax.transaction.Transactional

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Transactional
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = [(AutoConfigureTestDatabase::class)])
open class UtsattOppgaveDaoTest {

    @Autowired
    private lateinit var repository: UtsattOppgaveRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var utsattOppgaveDao: UtsattOppgaveDao

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
        utsattOppgaveDao = UtsattOppgaveDao(repository)
    }

    @Test
    fun `kan opprette oppgave`() {
        val oppgave1 = oppgave()
        utsattOppgaveDao.opprett(oppgave1)
        val oppgave = utsattOppgaveDao.finn(oppgave1.inntektsmeldingId)
        assertNotNull(oppgave)
        assertEquals(oppgave1.arkivreferanse, oppgave!!.arkivreferanse)
    }

    @Test
    fun `to fremtidige oppgaver har forskjellig id`() {
        val oppgave1 = oppgave()
        val oppgave2 = oppgave()
        val id1 = utsattOppgaveDao.opprett(oppgave1)
        val id2 = utsattOppgaveDao.opprett(oppgave2)

        val _oppgave1 = utsattOppgaveDao.finn(oppgave1.inntektsmeldingId)!!
        assertEquals(id1, _oppgave1.id)
        val _oppgave2 = utsattOppgaveDao.finn(oppgave2.inntektsmeldingId)!!
        assertEquals(id2, _oppgave2.id)
    }

    @Test
    fun `kan oppdatere tilstand til en oppgave`() {
        val oppgave1 = oppgave()
        val oppgave2 = oppgave()
        utsattOppgaveDao.opprett(oppgave1)
        utsattOppgaveDao.opprett(oppgave2)
        utsattOppgaveDao.save(oppgave1.copy(tilstand = Tilstand.Forkastet))

        val _oppgave1 = utsattOppgaveDao.finn(oppgave1.inntektsmeldingId)!!
        assertEquals(Tilstand.Forkastet, _oppgave1.tilstand)
        val _oppgave2 = utsattOppgaveDao.finn(oppgave2.inntektsmeldingId)!!
        assertEquals(Tilstand.Ny, _oppgave2.tilstand)
    }

    fun oppgave() = UtsattOppgaveEntitet(
        fnr = "fnr",
        sakId = "saksId",
        aktørId = "aktørId",
        journalpostId = "journalpostId",
        arkivreferanse = "arkivreferanse",
        timeout = now(),
        inntektsmeldingId = UUID.randomUUID().toString(),
        tilstand = Tilstand.Ny
    )
}
