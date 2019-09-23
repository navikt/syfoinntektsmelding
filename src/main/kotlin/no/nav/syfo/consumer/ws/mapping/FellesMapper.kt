package no.nav.syfo.consumer.ws.mapping

import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import javax.xml.bind.JAXBElement

fun mapNaturalytelseType(naturalytelseType: JAXBElement<String>) =
        naturalytelseType?.value?.let { n ->
            if (Naturalytelse.values().map { it.name }.contains(n.toUpperCase())) Naturalytelse.valueOf(n.toUpperCase()) else Naturalytelse.ANNET
        }
                ?: Naturalytelse.ANNET