package no.nav.helsearbeidsgiver.db

import no.nav.syfo.domain.inntektsmelding.Inntektsmelding

@Table("INNTEKTSMELDING")
data class Inntekt(
    @IdColumn(column="INNTEKTSMELDING_UUID")
    var id: String,

    @ValueColumn(column="NAME")
    var name: String,

    @JsonColumn(column="data")
    var inntektsmelding: Inntektsmelding? = null
)
