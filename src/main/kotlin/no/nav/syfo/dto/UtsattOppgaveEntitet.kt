package no.nav.syfo.dto

import java.time.LocalDateTime

data class UtsattOppgaveEntitet(
    var id: Int = 0,
    var inntektsmeldingId: String,
    var arkivreferanse: String,
    var fnr: String,
    var aktørId: String,
    var journalpostId: String,
    var timeout: LocalDateTime,
    var tilstand: Tilstand,
    var enhet: String = "",
    var gosysOppgaveId: String?,
    var oppdatert: LocalDateTime?,
    var speil: Boolean,
    var utbetalingBruker: Boolean,
)

enum class Tilstand {
    Utsatt,
    Forkastet,
    Opprettet,
    OpprettetTimeout,
}
