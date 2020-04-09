package no.nav.syfo.job

import no.nav.syfo.utsattoppgave.DokumentTypeDTO
import no.nav.syfo.utsattoppgave.OppdateringstypeDTO
import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import java.time.LocalDateTime
import java.util.UUID

class UtsattOppgave(
    var inntektsmeldingId: UUID,
    val tilstand: Tilstand,
    var timeout: LocalDateTime?
) {
    fun skalTimeUt() = (timeout?.isBefore(LocalDateTime.now()) ?: false)
        && (tilstand == Tilstand.Ny || tilstand == Tilstand.Utsatt)

    fun tilDTO() = UtsattOppgaveDTO(
        dokumentType = DokumentTypeDTO.Inntektsmelding,
        oppdateringstype = OppdateringstypeDTO.Opprett,
        dokumentId = inntektsmeldingId,
        timeout = null
    )
}

enum class Tilstand {
    Ny, Utsatt, Forkastet, Opprettet
}
