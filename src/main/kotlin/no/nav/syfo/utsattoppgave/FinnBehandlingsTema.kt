package no.nav.syfo.utsattoppgave

import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import java.math.BigDecimal

enum class BehandlingsKategori {
    SPEIL_RELATERT,
    UTLAND,
    IKKE_REFUSJON,
    REFUSJON_UTEN_DATO,
    REFUSJON_MED_DATO,
    REFUSJON_LITEN_LØNN,
    BETVILER_SYKEMELDING
}

fun finnBehandlingsKategori(inntektsmelding: Inntektsmelding, speilRelatert: Boolean, gjelderUtland: Boolean): BehandlingsKategori {

    if (speilRelatert) {
        return BehandlingsKategori.SPEIL_RELATERT
    }
    if (gjelderUtland) {
        return BehandlingsKategori.UTLAND
    }
    val refusjon = inntektsmelding.refusjon

    if (inntektsmelding.begrunnelseRedusert == "BetvilerArbeidsufoerhet") {
        return BehandlingsKategori.BETVILER_SYKEMELDING
    }

    if (refusjon.beloepPrMnd == null || refusjon.beloepPrMnd < BigDecimal(1)) {
        return BehandlingsKategori.IKKE_REFUSJON
    }

    if (refusjon.opphoersdato != null) {
        return BehandlingsKategori.REFUSJON_MED_DATO
    }

    if (refusjon.beloepPrMnd < inntektsmelding.beregnetInntekt) {
        return BehandlingsKategori.REFUSJON_LITEN_LØNN
    }

    return BehandlingsKategori.REFUSJON_UTEN_DATO
}
