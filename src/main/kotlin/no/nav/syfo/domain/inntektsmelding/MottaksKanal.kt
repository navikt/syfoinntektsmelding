package no.nav.syfo.domain.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Kanal

enum class MottaksKanal {
    NAV_NO,
    HR_SYSTEM_API,
    ALTINN,
}

fun Kanal.mapTilMottakskanal(): MottaksKanal =
    when (this) {
        Kanal.NAV_NO -> MottaksKanal.NAV_NO
        Kanal.HR_SYSTEM_API -> MottaksKanal.HR_SYSTEM_API
        Kanal.ALTINN -> MottaksKanal.ALTINN
    }
