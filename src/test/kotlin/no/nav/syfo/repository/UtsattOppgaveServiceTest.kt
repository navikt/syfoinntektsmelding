package no.nav.syfo.repository

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import no.nav.syfo.service.FremtidigOppgave
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
import java.util.*
import javax.transaction.Transactional

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Transactional
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = [(AutoConfigureTestDatabase::class)])
open class UtsattOppgaveServiceTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var repository: UtsattOppgaveRepository

    private lateinit var oppgaveService: UtsattOppgaveService

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
        oppgaveService = UtsattOppgaveService(repository)
    }

    @Test
    fun `kan opprette oppgave`() {
        val fnr = "fnr"
        val saksId = "saksId"
        val aktørId = "aktørId"
        val journalpostId = "journalpostId"
        val arkivreferanse = "123"
        val inntektsmeldingId = UUID.randomUUID()
        oppgaveService.opprett(
            FremtidigOppgave(
                fnr = fnr,
                saksId = saksId,
                aktørId = aktørId,
                journalpostId = journalpostId,
                arkivreferanse = arkivreferanse,
                timeout = now(),
                inntektsmeldingId = inntektsmeldingId
            )
        )
        val oppgave = oppgaveService.finn(inntektsmeldingId)
        assertNotNull(oppgave)
        assertEquals(arkivreferanse, oppgave!!.arkivreferanse)
    }

}
