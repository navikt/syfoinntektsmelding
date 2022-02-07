package no.nav.syfo.utsattoppgave

import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import java.math.BigDecimal

enum class BehandlingsTema(var navn: String) {
    IKKE_REFUSJON("1"),
    REFUSJON_UTEN_DATO("0"),
    REFUSJON_MED_DATO("2"),
    REFUSJON_LITEN_LØNN("3")
}

fun finnBehandlingsTema(inntektsmelding: Inntektsmelding): BehandlingsTema {
    val refusjon = inntektsmelding.refusjon
    if (refusjon.beloepPrMnd == null || refusjon.beloepPrMnd < BigDecimal(1)) {
        // 1). Ikke refusjon
        return BehandlingsTema.IKKE_REFUSJON
    } else {
        // Krever refusjon
        if (refusjon.opphoersdato == null) {
            // Uten dato
            if (refusjon.beloepPrMnd < inntektsmelding.beregnetInntekt) {
                // 3). Refusjon mindre enn månedslønn
                return BehandlingsTema.REFUSJON_LITEN_LØNN
            } else {
                // 0). Normal
                return BehandlingsTema.REFUSJON_UTEN_DATO
            }
        } else {
            // 2). Med dato
            return BehandlingsTema.REFUSJON_MED_DATO
        }
    }
}

