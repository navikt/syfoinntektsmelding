package no.nav.syfo.consumer.ws.mapping

import no.nav.syfo.domain.inntektsmelding.Naturalytelse
import java.util.Locale
import javax.xml.bind.JAXBElement

fun mapNaturalytelseType(naturalytelseType: JAXBElement<String>) =
    naturalytelseType?.value?.let { n ->
        if (Naturalytelse.values().map { it.name }.contains(n.uppercase(Locale.getDefault()))) Naturalytelse.valueOf(n.uppercase(Locale.getDefault())) else Naturalytelse.ANNET
    }
        ?: Naturalytelse.ANNET
