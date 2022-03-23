package no.nav.syfo.mapping

import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import javax.xml.bind.JAXBElement

fun mapNaturalytelseType(naturalytelseType: JAXBElement<String>) =
    naturalytelseType.value?.let { n ->
        if (Naturalytelse.values().map { it.name }.contains(n.uppercase())) Naturalytelse.valueOf(n.uppercase()) else Naturalytelse.ANNET
    }
        ?: Naturalytelse.ANNET
