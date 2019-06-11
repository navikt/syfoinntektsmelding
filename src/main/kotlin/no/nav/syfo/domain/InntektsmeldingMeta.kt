package no.nav.syfo.domain

import java.time.LocalDateTime

data class InntektsmeldingMeta (
    val uuid: String? = null,
    val aktorId: String,
    val sakId: String,
    val journalpostId: String,
    val orgnummer: String? = null,
    val arbeidsgiverPrivat: String? = null,
    val behandlet: LocalDateTime? = null,
    val arbeidsgiverperioder: List<Periode> = emptyList()
)
