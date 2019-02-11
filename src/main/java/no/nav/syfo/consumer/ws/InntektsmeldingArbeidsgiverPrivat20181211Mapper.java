package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Periode;
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLInntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLSkjemainnhold;
import no.seres.xsd.nav.inntektsmelding_m._20181211.XMLArbeidsgiverperiodeListe;

import javax.xml.bind.JAXBElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
class InntektsmeldingArbeidsgiverPrivat20181211Mapper {

    static XMLInntektsmelding tilXMLInntektsmelding(JAXBElement<Object> jaxbInntektsmelding) {
        log.info("Behandling inntektsmelding på 20181211 format");
        XMLSkjemainnhold skjemainnhold = ((XMLInntektsmeldingM) jaxbInntektsmelding.getValue()).getSkjemainnhold();

        XMLInntektsmelding xmlInntektsmelding;
        String arbeidsforholdId = Optional.ofNullable(skjemainnhold.getArbeidsforhold().getValue().getArbeidsforholdId())
                .map(JAXBElement::getValue)
                .orElse(null);

        List<Periode> perioder = Stream.of(skjemainnhold.getSykepengerIArbeidsgiverperioden().getValue().getArbeidsgiverperiodeListe())
                .map(JAXBElement::getValue)
                .filter(Objects::nonNull)
                .map(XMLArbeidsgiverperiodeListe::getArbeidsgiverperiode)
                .flatMap(List::stream)
                .filter(xmlPeriode -> xmlPeriode.getFom() != null && xmlPeriode.getTom() != null)
                .map(p -> Periode.builder()
                        .fom(p.getFom().getValue())
                        .tom(p.getTom().getValue())
                        .build())
                .collect(Collectors.toList());

        xmlInntektsmelding = new XMLInntektsmelding(
                arbeidsforholdId,
                perioder,
                skjemainnhold.getArbeidstakerFnr(),
                Optional.ofNullable(skjemainnhold.getArbeidsgiver()).map(arbeidsgiver -> arbeidsgiver.getValue().getVirksomhetsnummer()),
                Optional.ofNullable(skjemainnhold.getArbeidsgiverPrivat()).map(arbeidsgiverPrivat -> arbeidsgiverPrivat.getValue().getArbeidsgiverFnr()),
                skjemainnhold.getAarsakTilInnsending()
        );
        return xmlInntektsmelding;
    }
}
