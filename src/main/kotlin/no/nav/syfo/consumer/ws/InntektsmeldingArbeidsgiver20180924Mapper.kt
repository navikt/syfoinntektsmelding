package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.Periode
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLArbeidsgiver
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLInntektsmeldingM
import java.util.Optional
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.xml.bind.JAXBElement


internal object InntektsmeldingArbeidsgiver20180924Mapper {

    val log = log()

    fun tilXMLInntektsmelding(jabxInntektsmelding: JAXBElement<Any>): XMLInntektsmelding {
        log.info("Behandling inntektsmelding på 20180924 format")
        val skjemainnhold = (jabxInntektsmelding.value as XMLInntektsmeldingM).skjemainnhold

        val xmlInntektsmelding: XMLInntektsmelding
        val arbeidsforholdId = skjemainnhold.arbeidsforhold.value.arbeidsforholdId?.value

        val perioder = Stream.of(skjemainnhold.sykepengerIArbeidsgiverperioden.value.arbeidsgiverperiodeListe)
            .map { it.value }
            .filter { it != null }
            .map { it.arbeidsgiverperiode }
            .flatMap { it.stream() }
            .filter { xmlPeriode -> xmlPeriode.fom != null && xmlPeriode.tom != null }
            .map { Periode(it.fom.value, it.tom.value) }
            .collect(Collectors.toList())

        xmlInntektsmelding = XMLInntektsmelding(
            arbeidsforholdId,
            perioder,
            skjemainnhold.arbeidstakerFnr,
            skjemainnhold.arbeidsgiver?.virksomhetsnummer,
            null,
            skjemainnhold.aarsakTilInnsending
        )
        return xmlInntektsmelding
    }
}
