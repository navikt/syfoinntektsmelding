package no.nav.syfo.utsattoppgave

import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import java.math.BigDecimal

enum class BehandlingsTema{
    IKKE_REFUSJON,
    REFUSJON_UTEN_DATO,
    REFUSJON_MED_DATO,
    REFUSJON_LITEN_LØNN
}

fun finnBehandlingsTema(inntektsmelding: Inntektsmelding): BehandlingsTema {
    val refusjon = inntektsmelding.refusjon

    if (refusjon.beloepPrMnd == null || refusjon.beloepPrMnd < BigDecimal(1)) {
        return BehandlingsTema.IKKE_REFUSJON
    }

    if (refusjon.opphoersdato != null) {
        return BehandlingsTema.REFUSJON_MED_DATO
    }

    if (refusjon.beloepPrMnd < inntektsmelding.beregnetInntekt) {
        return BehandlingsTema.REFUSJON_LITEN_LØNN
    }

    return BehandlingsTema.REFUSJON_UTEN_DATO
}
