package no.nav.syfo.repository

import junit.framework.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.Before
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
import javax.transaction.Transactional

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Transactional
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = [(AutoConfigureTestDatabase::class)])
open class UtsattOppgaveDAOTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var repository: UtsattOppgaveRepository

    private lateinit var oppgaveDao: UtsattOppgaveDAO

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
        oppgaveDao = UtsattOppgaveDAO(repository)
    }

    @Test
    fun `kan opprette oppgave`() {
        val arkivreferanse = "123"
        oppgaveDao.opprett(arkivreferanse = arkivreferanse, timeout = now())
        val oppgave = oppgaveDao.finn(arkivreferanse)
        assertNotNull(oppgave)
        assertEquals(arkivreferanse, oppgave!!.arkivreferanse)
    }

    @Test
    fun `kan slette oppgave`() {
        val arkivreferanse = "123"
        oppgaveDao.opprett(arkivreferanse = arkivreferanse, timeout = now())
        oppgaveDao.slett(arkivreferanse)
        val oppgave = oppgaveDao.finn(arkivreferanse)
        assertNull(oppgave)
    }

}
