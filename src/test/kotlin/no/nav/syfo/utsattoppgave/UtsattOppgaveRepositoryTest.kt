package no.nav.syfo.utsattoppgave

import junit.framework.Assert.assertEquals
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository
import org.junit.Before
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
@EnableAutoConfiguration(exclude = arrayOf((AutoConfigureTestDatabase::class)))
open class UtsattOppgaveRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var repository: UtsattOppgaveRepository

    @Before
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `finner en utsatt oppgave som skal opprettes`() {
        repository.save(
            UtsattOppgaveEntitet(
                id = 0,
                inntektsmeldingId = "id",
                arkivreferanse = "arkivref",
                fnr = "fnr",
                aktørId = "aktørId",
                sakId = "sakId",
                journalpostId = "journalpostId",
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt
            )
        )

        val oppgaver =
            repository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(now(), Tilstand.Utsatt)

        assertEquals(1, oppgaver.size)
    }

    @Test
    fun `ignorerer oppgaver i andre tilstander`() {
        repository.save(
            enOppgaveEntitet(
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                inntektsmeldingsId = "id1"
            )
        )

        repository.save(
            enOppgaveEntitet(
                timeout = now().minusHours(1),
                tilstand = Tilstand.Forkastet,
                inntektsmeldingsId = "id2"
            )
        )
        repository.save(
            enOppgaveEntitet(
                timeout = now().plusHours(1),
                tilstand = Tilstand.Utsatt,
                inntektsmeldingsId = "id3"
            )
        )

        repository.save(
            enOppgaveEntitet(
                timeout = now().minusHours(1),
                tilstand = Tilstand.Opprettet,
                inntektsmeldingsId = "id4"
            ))

            val oppgaver =
        repository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(now(), Tilstand.Utsatt)

        assertEquals(1, oppgaver.size)
    }

    private fun enOppgaveEntitet(
        timeout: LocalDateTime,
        tilstand: Tilstand = Tilstand.Utsatt,
        inntektsmeldingsId: String = UUID.randomUUID().toString()
    ) = UtsattOppgaveEntitet(
        fnr = "fnr",
        sakId = "saksId",
        aktørId = "aktørId",
        journalpostId = "journalpostId",
        arkivreferanse = "arkivreferanse",
        timeout = timeout,
        inntektsmeldingId = inntektsmeldingsId,
        tilstand = tilstand
    )
}
