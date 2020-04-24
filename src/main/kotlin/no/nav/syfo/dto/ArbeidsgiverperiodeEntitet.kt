package no.nav.syfo.dto

import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDate
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "ARBEIDSGIVERPERIODE")
data class ArbeidsgiverperiodeEntitet (

    @Id
    @Column(name = "PERIODE_UUID", nullable = false, length = 100, updatable = false)
    var uuid: String = UUID.randomUUID().toString(),

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "INNTEKTSMELDING_UUID", nullable = false)
    var inntektsmelding : InntektsmeldingEntitet? = null,

    @Column(name = "FOM", nullable = false)
    var fom: LocalDate,

    @Column(name = "TOM", nullable = false)
    var tom: LocalDate

)
