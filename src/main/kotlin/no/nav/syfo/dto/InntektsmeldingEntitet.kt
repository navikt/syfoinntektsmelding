package no.nav.syfo.dto

import javax.persistence.*
import java.time.*
import java.util.*
import javax.persistence.CascadeType.ALL

@Entity
@Table(name = "INNTEKTSMELDING")
data class InntektsmeldingEntitet (

        @Id
        @Column(name = "INNTEKTSMELDING_UUID", length = 100, updatable = false)
        var uuid: String? = UUID.randomUUID().toString(),

        @Column(name = "AKTOR_ID", nullable = false)
        var aktorId: String,

        @Column(name = "SAK_ID", nullable = false, length = 50)
        var sakId: String,

        @Column(name = "JOURNALPOST_ID", length = 100)
        var journalpostId: String,

        @Column(name = "ORGNUMMER", nullable = true, length = 50)
        var orgnummer: String? = null,

        @Column(name = "ARBEIDSGIVER_PRIVAT", nullable = true, length = 50)
        var arbeidsgiverPrivat: String? = null,

        @Column(name = "BEHANDLET")
        var behandlet: LocalDateTime? = LocalDateTime.now()

){

        @OneToMany(mappedBy = "inntektsmelding", cascade = [ALL], orphanRemoval = true, fetch = FetchType.EAGER)
        val arbeidsgiverperioder: MutableList<ArbeidsgiverperiodeEntitet> = ArrayList()

        fun leggtilArbeidsgiverperiode(fom: LocalDate, tom: LocalDate){
                val periode = ArbeidsgiverperiodeEntitet()
                periode.fom = fom
                periode.tom = tom
                periode.inntektsmelding = this
                arbeidsgiverperioder.add(periode)
        }

}
