package no.nav.syfo.consumer.ws.mapping

import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import javax.xml.bind.JAXBElement

fun mapNaturalytelseType(naturalytelseType: JAXBElement<String>) =
        naturalytelseType?.value?.let { n ->
            if (Naturalytelse.values().map { it.name }.contains(n)) Naturalytelse.valueOf(n) else Naturalytelse.annet
        }
                ?: Naturalytelse.annet