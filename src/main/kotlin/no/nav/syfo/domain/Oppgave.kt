package no.nav.syfo.domain

import java.time.LocalDate

data class Oppgave(
    val oppgaveId: String? = null,
    val versjon: Int = 0,
    val beskrivelse: String? = null,
    val aktivFra: LocalDate? = null,
    val aktivTil: LocalDate? = null,
    val oppgavetype: String? = null,
    val fagomrade: String? = null,
    val prioritet: String? = null,
    val ansvarligEnhetId: String? = null,
    val saksnummer: String? = null,
    val dokumentId: String? = null,
    val status: String? = null,
    val geografiskTilknytning: String? = null
)
