package no.nav.syfo.slowtests.repository

import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository
import no.nav.syfo.repository.UtsattOppgaveRepositoryImp
import no.nav.syfo.repository.createTestHikariConfig
import no.nav.syfo.slowtests.SystemTestBase
import no.nav.syfo.grunnleggendeInntektsmelding
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

open class UtsattOppgaveRepositoryImpTest : SystemTestBase() {

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
        repository.opprett(
            UtsattOppgaveEntitet(
                id = 0,
                inntektsmeldingId = "id",
                arkivreferanse = "arkivref",
                fnr = "fnr",
                aktørId = "aktørId",
                sakId = "sakId",
                journalpostId = "journalpostId",
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                gosysOppgaveId = null,
                oppdatert = null,
                speil = false,
                utbetalingBruker = false
            )
        )

        val oppgaver =
            repository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(now(), Tilstand.Utsatt)

        Assertions.assertEquals(1, oppgaver.size)
    }

    @Test
    fun `Databasen angir ID ved opprettelse`() {
        val firstId = repository.opprett(
            UtsattOppgaveEntitet(
                inntektsmeldingId = "id",
                arkivreferanse = "arkivref",
                fnr = "fnr",
                aktørId = "aktørId",
                sakId = "sakId",
                journalpostId = "journalpostId",
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                gosysOppgaveId = null,
                oppdatert = null,
                speil = false,
                utbetalingBruker = false
            )
        ).id

        val secondId = repository.opprett(
            UtsattOppgaveEntitet(
                inntektsmeldingId = "id2",
                arkivreferanse = "arkivref",
                fnr = "fnr",
                aktørId = "aktørId",
                sakId = "sakId",
                journalpostId = "journalpostId",
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                gosysOppgaveId = null,
                oppdatert = null,
                speil = false,
                utbetalingBruker = false
            )
        ).id

        Assertions.assertNotEquals(firstId, secondId)
    }

    @Test
    fun `oppdaterer oppgaver med enhet`() {
        val enhet = "Test123"

        val utsattOppgave = repository.opprett(
            UtsattOppgaveEntitet(
                inntektsmeldingId = "id3",
                arkivreferanse = "arkivref",
                fnr = "fnr",
                aktørId = "aktørId",
                sakId = "sakId",
                journalpostId = "journalpostId",
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                enhet = "",
                gosysOppgaveId = null,
                oppdatert = null,
                speil = false,
                utbetalingBruker = false
            )
        )

        utsattOppgave.enhet = enhet
        repository.oppdater(utsattOppgave)

        val enhetDb = repository.findByInntektsmeldingId(utsattOppgave.inntektsmeldingId)?.enhet
        Assertions.assertEquals(enhet, enhetDb)
    }

    @Test
    fun `ignorerer oppgaver i andre tilstander`() {
        repository.opprett(
            enOppgaveEntitet(
                id = 0,
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                inntektsmeldingsId = "id1"
            )
        )

        repository.opprett(
            enOppgaveEntitet(
                id = 1,
                timeout = now().minusHours(1),
                tilstand = Tilstand.Forkastet,
                inntektsmeldingsId = "id2"
            )
        )
        repository.opprett(
            enOppgaveEntitet(
                id = 2,
                timeout = now().plusHours(1),
                tilstand = Tilstand.Utsatt,
                inntektsmeldingsId = "id3"
            )
        )

        repository.opprett(
            enOppgaveEntitet(
                id = 3,
                timeout = now().minusHours(1),
                tilstand = Tilstand.Opprettet,
                inntektsmeldingsId = "id4"
            )
        )

        val oppgaver =
            repository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(now(), Tilstand.Utsatt)

        Assertions.assertEquals(1, oppgaver.size)
    }

    @Test
    fun `hent oppgaver med og uten oppdatert felt`() {
        repository.opprett(
            UtsattOppgaveEntitet(
                id = 0,
                inntektsmeldingId = "id",
                arkivreferanse = "arkivref",
                fnr = "fnr",
                aktørId = "aktørId",
                sakId = "sakId",
                journalpostId = "journalpostId",
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                gosysOppgaveId = null,
                oppdatert = null,
                speil = false,
                utbetalingBruker = false
            )
        )

        repository.opprett(
            UtsattOppgaveEntitet(
                id = 1,
                inntektsmeldingId = "id2",
                arkivreferanse = "arkivref2",
                fnr = "fnr2",
                aktørId = "aktørId2",
                sakId = "sakId2",
                journalpostId = "journalpostId2",
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                oppdatert = null,
                gosysOppgaveId = null,
                speil = false,
                utbetalingBruker = false
            )
        )

        val oppgaver =
            repository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(now(), Tilstand.Utsatt)

        Assertions.assertEquals(2, oppgaver.size)
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
        tilstand = tilstand,
        enhet = "",
        gosysOppgaveId = null,
        oppdatert = null,
        speil = false,
        utbetalingBruker = false
    )
}
