package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.Periode
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLInntektsmeldingM
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.xml.bind.JAXBElement

internal object InntektsmeldingArbeidsgiverPrivat20181211Mapper {

    val log = log()

    fun tilXMLInntektsmelding(jaxbInntektsmelding: JAXBElement<Any>): XMLInntektsmelding {
        log.info("Behandling inntektsmelding på 20181211 format")
        val skjemainnhold = (jaxbInntektsmelding.value as XMLInntektsmeldingM).skjemainnhold

        val xmlInntektsmelding: XMLInntektsmelding
        val arbeidsforholdId = skjemainnhold.arbeidsforhold.value.arbeidsforholdId?.value

        val perioder = skjemainnhold.sykepengerIArbeidsgiverperioden.value.arbeidsgiverperiodeListe
            .value
            ?.arbeidsgiverperiode
            ?.filter { xmlPeriode -> xmlPeriode.fom != null && xmlPeriode.tom != null }
            ?.map { Periode(it.fom.value, it.tom.value) }
            ?: emptyList()

        xmlInntektsmelding = XMLInntektsmelding(
            arbeidsforholdId,
            perioder,
            skjemainnhold.arbeidstakerFnr,
            skjemainnhold.arbeidsgiver?.value?.virksomhetsnummer,
            skjemainnhold.arbeidsgiverPrivat?.value?.arbeidsgiverFnr,
            skjemainnhold.aarsakTilInnsending
        )
        return xmlInntektsmelding
    }
}
