package no.nav.syfo.utsattoppgave

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
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
import java.time.LocalDateTime
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

    private val fnr = "fnr"
    private val saksId = "saksId"
    private val aktørId = "aktørId"
    private val journalpostId = "journalpostId"
    private val arkivreferanse = "123"

    @Test
    fun `kan opprette oppgave`() {
        val uuid = UUID.randomUUID()
        utsattOppgaveDao.opprett(
            FremtidigOppgave(
                fnr = fnr,
                saksId = saksId,
                aktørId = aktørId,
                journalpostId = journalpostId,
                arkivreferanse = arkivreferanse,
                timeout = now(),
                inntektsmeldingId = uuid
            )
        )
        val oppgave = utsattOppgaveDao.finn(uuid)
        assertNotNull(oppgave)
        assertEquals(arkivreferanse, oppgave!!.arkivreferanse)
    }

    @Test
    fun `to fremtidige oppgaver har forskjellig id`() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val id1 = utsattOppgaveDao.opprett(
            FremtidigOppgave(
                fnr = fnr,
                saksId = saksId,
                aktørId = aktørId,
                journalpostId = journalpostId,
                arkivreferanse = arkivreferanse,
                timeout = now(),
                inntektsmeldingId = uuid1
            )
        )
        val id2 = utsattOppgaveDao.opprett(
            FremtidigOppgave(
                fnr = fnr,
                saksId = saksId,
                aktørId = aktørId,
                journalpostId = journalpostId,
                arkivreferanse = arkivreferanse,
                timeout = now(),
                inntektsmeldingId = uuid2
            )
        )

        val oppgave1 = utsattOppgaveDao.finn(uuid1)!!
        assertEquals(id1, oppgave1.id)
        val oppgave2 = utsattOppgaveDao.finn(uuid2)!!
        assertEquals(id2, oppgave2.id)
    }

}
