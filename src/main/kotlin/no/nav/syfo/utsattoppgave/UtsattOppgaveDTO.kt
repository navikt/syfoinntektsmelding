package no.nav.syfo.utsattoppgave

import java.time.LocalDateTime
import java.util.UUID

data class UtsattOppgaveDTO(
    val dokumentType: DokumentTypeDTO,
    val oppdateringstype: OppdateringstypeDTO,
    val dokumentId: UUID,
    val timeout: LocalDateTime? = null
)

enum class OppdateringstypeDTO {
    Utsett, Opprett, OpprettSpeilRelatert, Ferdigbehandlet
}

enum class DokumentTypeDTO {
    Inntektsmelding, Søknad
}

fun OppdateringstypeDTO.tilHandling() = when (this) {
    OppdateringstypeDTO.Utsett -> Handling.Utsett
    OppdateringstypeDTO.Opprett,
    OppdateringstypeDTO.OpprettSpeilRelatert -> Handling.Opprett
    OppdateringstypeDTO.Ferdigbehandlet -> Handling.Forkast
}

fun OppdateringstypeDTO.erSpeilRelatert() = when (this) {
    OppdateringstypeDTO.OpprettSpeilRelatert -> true
    else -> false
}
