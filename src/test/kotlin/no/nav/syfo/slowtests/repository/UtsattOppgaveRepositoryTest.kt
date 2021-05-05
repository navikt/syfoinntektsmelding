package no.nav.syfo.slowtests.repository

import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.grunnleggendeInntektsmelding
import no.nav.syfo.repository.*
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.jupiter.api.*
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

open class UtsattOppgaveRepositoryTest : SystemTestBase(){

    lateinit var repository: UtsattOppgaveRepository

    val testKrav = grunnleggendeInntektsmelding

    @BeforeAll
    fun setUp() {
        val ds = HikariDataSource(createTestHikariConfig())
        repository = UtsattOppgaveRepositoryImp(ds)
    }

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `finner en utsatt oppgave som skal opprettes`() {
        repository.lagreInnteksmelding(
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

       Assertions.assertEquals(1, oppgaver.size)
    }

    @Test
    fun `ignorerer oppgaver i andre tilstander`() {
        repository.lagreInnteksmelding(
            enOppgaveEntitet(
                id= 0,
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                inntektsmeldingsId = "id1"
            )
        )

        repository.lagreInnteksmelding(
            enOppgaveEntitet(
                id= 1,
                timeout = now().minusHours(1),
                tilstand = Tilstand.Forkastet,
                inntektsmeldingsId = "id2"
            )
        )
        repository.lagreInnteksmelding(
            enOppgaveEntitet(
                id= 2,
                timeout = now().plusHours(1),
                tilstand = Tilstand.Utsatt,
                inntektsmeldingsId = "id3"
            )
        )

        repository.lagreInnteksmelding(
            enOppgaveEntitet(
                id= 3,
                timeout = now().minusHours(1),
                tilstand = Tilstand.Opprettet,
                inntektsmeldingsId = "id4"
            ))

            val oppgaver =
        repository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(now(), Tilstand.Utsatt)

        Assertions.assertEquals(1, oppgaver.size)
    }

    private fun enOppgaveEntitet(
        id: Int,
        timeout: LocalDateTime,
        tilstand: Tilstand = Tilstand.Utsatt,
        inntektsmeldingsId: String = UUID.randomUUID().toString()
    ) = UtsattOppgaveEntitet(
        id = id,
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

