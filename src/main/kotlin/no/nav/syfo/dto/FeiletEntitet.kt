package no.nav.syfo.dto

import no.nav.syfo.behandling.Feiltype
import java.time.LocalDateTime

data class FeiletEntitet(
    var id: Int = 0,
    var arkivReferanse: String,
    var tidspunkt: LocalDateTime = LocalDateTime.now(),
    var feiltype: Feiltype
)
