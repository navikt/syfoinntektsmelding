package no.nav.syfo.domain

import lombok.Builder
import lombok.Getter
import lombok.Value


data class Sykmelding (
    val id: Int,
    val orgnummer: String? = null
)
