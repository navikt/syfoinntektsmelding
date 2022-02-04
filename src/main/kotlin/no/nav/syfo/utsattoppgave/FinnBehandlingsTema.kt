package no.nav.syfo.utsattoppgave

import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import java.math.BigDecimal

fun finnBehandlingsTema(inntektsmelding: Inntektsmelding): String {
    val refusjon = inntektsmelding.refusjon
    if (refusjon.beloepPrMnd == null || refusjon.beloepPrMnd < BigDecimal(1)) {
        // 1). Ikke refusjon
        return "1"
    } else {
        // Krever refusjon
        if (refusjon.opphoersdato == null) {
            // Uten dato
            if (refusjon.beloepPrMnd < inntektsmelding.beregnetInntekt) {
                // 3). Refusjon mindre enn månedslønn
                return "3"
            } else {
                // 0). Normal
                return "0"
            }
        } else {
            // 2). Med dato
            return "2"
        }
    }
}
