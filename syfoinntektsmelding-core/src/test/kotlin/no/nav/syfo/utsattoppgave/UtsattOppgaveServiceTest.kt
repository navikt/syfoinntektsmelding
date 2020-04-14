package no.nav.syfo.utsattoppgave

import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.util.UUID
import javax.transaction.Transactional

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Transactional
@DataJpaTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = [(AutoConfigureTestDatabase::class)])
open class UtsattOppgaveServiceTest {

    private var utsattOppgaveDao: UtsattOppgaveDao = mockk(relaxed = true)
    private var oppgaveClient: OppgaveClient = mockk()
    private var behandlendeEnhetConsumer: BehandlendeEnhetConsumer = mockk()

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
        oppgaveService = UtsattOppgaveService(utsattOppgaveDao, oppgaveClient, behandlendeEnhetConsumer)
    }

    private val fnr = "fnr"
    private val saksId = "saksId"
    private val aktørId = "aktørId"
    private val journalpostId = "journalpostId"
    private val arkivreferanse = "123"

    @Test
    fun `oppretter forsinket oppgave med timeout`() {
        val timeout = LocalDateTime.of(2020, 4, 6, 9, 0)
        val oppgave = UtsattOppgaveEntitet(
            fnr = fnr,
            sakId = saksId,
            aktørId = aktørId,
            journalpostId = journalpostId,
            arkivreferanse = arkivreferanse,
            timeout = timeout,
            inntektsmeldingId = UUID.randomUUID().toString(),
            tilstand = Tilstand.Ny
        )
        oppgaveService.opprett(oppgave)
        verify { utsattOppgaveDao.opprett(oppgave) }
    }

}
