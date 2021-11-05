package no.nav.syfo.dto

import java.time.LocalDateTime
import no.nav.syfo.behandling.Feiltype

data class FeiletEntitet(
    var id: Int = 0,
    var arkivReferanse: String,
    var tidspunkt: LocalDateTime = LocalDateTime.now(),
    var feiltype: Feiltype
)
