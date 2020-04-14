package no.nav.syfo.dto

import no.nav.syfo.behandling.Feiltype
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "FEILET")
data class FeiletEntitet (

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "FEILET_ID", nullable = false, length = 100, updatable = false)
    var id: Int = 0,

    @Column(name = "ARKIVREFERANSE", nullable = false, length = 100, updatable = false)
    var arkivReferanse: String,

    @Column(name = "TIDSPUNKT", nullable = false)
    var tidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "FEILTYPE", nullable = false)
    var feiltype: Feiltype

)
