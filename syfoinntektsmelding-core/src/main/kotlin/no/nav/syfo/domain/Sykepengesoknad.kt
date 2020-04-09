package no.nav.syfo.domain

import lombok.Builder
import lombok.Getter
import lombok.Value

import java.time.LocalDate

data class Sykepengesoknad (
    val uuid: String? = null,
    val status: String? = null,
    val saksId: String? = null,
    val oppgaveId: String? = null,
    val journalpostId: String? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null
)
