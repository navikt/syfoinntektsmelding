package no.nav.syfo.consumer.rest.norg

import java.time.LocalDate

data class ArbeidsfordelingResponse(
    val behandlingstema: String,
    val behandlingstype: String,
    val diskresjonskode: String,
    val enhetId: Int,
    val enhetNavn: String,
    val enhetNr: String,
    val geografiskOmraade: String,
    val gyldigFra: LocalDate,
    val gyldigTil: LocalDate,
    val id: Int,
    val oppgavetype: String,
    val skalTilLokalkontor: Boolean,
    val tema: String,
    val temagruppe: String,
)
