package no.nav.syfo.dto

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "UTSATT_OPPGAVE")
data class UtsattOppgaveEntitet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OPPGAVE_ID", nullable = false, length = 100, updatable = false)
    var id: Int = 0,

    @Column(name = "INNTEKTSMELDING_ID", nullable = false, length = 100, updatable = false)
    var inntektsmeldingId: String,

    @Column(name = "ARKIVREFERANSE", nullable = false, length = 100, updatable = false)
    var arkivreferanse: String,

    @Column(name = "FNR", nullable = false, length = 50, updatable = false)
    var fnr: String,

    @Column(name = "AKTOR_ID", nullable = false, length = 50, updatable = false)
    var aktørId: String,

    @Column(name = "SAK_ID", nullable = false, length = 100, updatable = false)
    var sakId: String,

    @Column(name = "JOURNALPOST_ID", nullable = false, length = 100, updatable = false)
    var journalpostId: String,

    @Column(name = "TIMEOUT", nullable = false, updatable = false)
    var timeout: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "TILSTAND", nullable = false, length = 100, updatable = false)
    var tilstand: Tilstand
)

enum class Tilstand {
    Utsatt, Forkastet, Opprettet
}
