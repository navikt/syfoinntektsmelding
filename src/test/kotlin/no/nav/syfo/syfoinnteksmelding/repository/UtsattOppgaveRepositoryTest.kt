package no.nav.syfo.syfoinnteksmelding.repository

import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepositoryMockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

open class UtsattOppgaveRepositoryTest {

    private var repository = UtsattOppgaveRepositoryMockk()

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
                timeout = now().minusHours(1),
                tilstand = Tilstand.Utsatt,
                inntektsmeldingsId = "id1"
            )
        )

        repository.lagreInnteksmelding(
            enOppgaveEntitet(
                timeout = now().minusHours(1),
                tilstand = Tilstand.Forkastet,
                inntektsmeldingsId = "id2"
            )
        )
        repository.lagreInnteksmelding(
            enOppgaveEntitet(
                timeout = now().plusHours(1),
                tilstand = Tilstand.Utsatt,
                inntektsmeldingsId = "id3"
            )
        )

        repository.lagreInnteksmelding(
            enOppgaveEntitet(
                timeout = now().minusHours(1),
                tilstand = Tilstand.Opprettet,
                inntektsmeldingsId = "id4"
            ))

            val oppgaver =
        repository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(now(), Tilstand.Utsatt)

        Assertions.assertEquals(1, oppgaver.size)
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

