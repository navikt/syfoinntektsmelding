package no.nav.syfo.dto

import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "ARBEIDSGIVERPERIODE")
data class ArbeidsgiverperiodeEntitet (

    @Id
        @Column(name = "PERIODE_UUID", nullable = false, length = 100, updatable = false)
        var uuid: UUID = UUID.randomUUID(),

    @ManyToOne
        @JoinColumn(name="INNTEKTSMELDING_UUID", nullable=false)
        var inntektsmelding : InntektsmeldingEntitet? = null,

    @Column(name = "FOM", nullable = false)
        var fom: LocalDate? = null,

    @Column(name = "tom", nullable = false)
        var tom: LocalDate? = null

)
