package no.nav.syfo.service

import no.nav.syfo.client.norg.ArbeidsfordelingResponse
import java.time.LocalDate

fun buildArbeidsfordelingResponse(enhetNr: String, fra: LocalDate, til: LocalDate, status: String = "Aktiv"): ArbeidsfordelingResponse {
    return ArbeidsfordelingResponse(
        aktiveringsdato = fra,
        antallRessurser = 0,
        enhetId = 123456789,
        enhetNr = enhetNr,
        kanalstrategi = null,
        navn = "NAV Område",
        nedleggelsesdato = til,
        oppgavebehandler = false,
        orgNivaa = "SPESEN",
        orgNrTilKommunaltNavKontor = "",
        organisasjonsnummer = null,
        sosialeTjenester = "",
        status = status,
        type = "KO",
        underAvviklingDato = null,
        underEtableringDato = LocalDate.of(2020, 12, 30),
        versjon = 1
    )
}
