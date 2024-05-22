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

fun finnBehandlingsKategori(inntektsmelding: Inntektsmelding, speilRelatert: Boolean, gjelderUtland: Boolean): BehandlingsKategori =
    when {
        speilRelatert -> BehandlingsKategori.SPEIL_RELATERT
        gjelderUtland -> BehandlingsKategori.UTLAND
        inntektsmelding.begrunnelseRedusert == "BetvilerArbeidsufoerhet" -> BehandlingsKategori.BETVILER_SYKEMELDING
        inntektsmelding.refusjon.beloepPrMnd == null || inntektsmelding.refusjon.beloepPrMnd < BigDecimal(1) -> BehandlingsKategori.IKKE_REFUSJON
        inntektsmelding.refusjon.opphoersdato != null -> BehandlingsKategori.REFUSJON_MED_DATO
        inntektsmelding.refusjon.beloepPrMnd < inntektsmelding.beregnetInntekt -> BehandlingsKategori.REFUSJON_LITEN_LØNN
        else -> BehandlingsKategori.REFUSJON_UTEN_DATO
    }
