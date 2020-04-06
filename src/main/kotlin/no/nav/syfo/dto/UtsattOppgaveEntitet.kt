package no.nav.syfo.dto

import javax.persistence.*

@Entity
@Table(name = "UTSATT_OPPGAVE")
data class UtsattOppgaveEntitet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OPPGAVE_ID", nullable = false, length = 100, updatable = false)
    var id: Int = 0,

    @Column(name = "ARKIVREFERANSE", nullable = false, length = 100, updatable = false)
    var arkivreferanse: String
)
