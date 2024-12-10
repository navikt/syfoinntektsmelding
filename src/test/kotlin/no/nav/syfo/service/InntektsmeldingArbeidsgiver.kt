package no.nav.syfo.syfoinntektsmelding.consumer.ws

import no.nav.syfo.domain.Periode
import java.time.format.DateTimeFormatter
import java.util.function.BinaryOperator

fun inntektsmeldingArbeidsgiver(
    perioder: List<Periode>,
    fnr: String = "fnr",
): String {
    return "<ns6:melding xmlns:ns6=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180924\">" +
        "    <ns6:Skjemainnhold>" +
        "        <ns6:ytelse>Sykepenger</ns6:ytelse>" +
        "        <ns6:aarsakTilInnsending>Ny</ns6:aarsakTilInnsending>" +
        "        <ns6:arbeidsgiver>" +
        "            <ns6:virksomhetsnummer>orgnummer</ns6:virksomhetsnummer>" +
        "            <ns6:kontaktinformasjon>" +
        "                <ns6:kontaktinformasjonNavn>Ingelin Haugsdal</ns6:kontaktinformasjonNavn>" +
        "                <ns6:telefonnummer>81549300</ns6:telefonnummer>" +
        "            </ns6:kontaktinformasjon>" +
        "        </ns6:arbeidsgiver>" +
        "        <ns6:arbeidstakerFnr>" + fnr + "</ns6:arbeidstakerFnr>" +
        "        <ns6:naerRelasjon>false</ns6:naerRelasjon>" +
        "        <ns6:arbeidsforhold>" +
        "            <ns6:foersteFravaersdag>2019-02-01</ns6:foersteFravaersdag>" +
        "            <ns6:beregnetInntekt>" +
        "                <ns6:beloep>2000</ns6:beloep>" +
        "            </ns6:beregnetInntekt>" +
        "            <ns6:avtaltFerieListe/>" +
        "            <ns6:utsettelseAvForeldrepengerListe/>" +
        "            <ns6:graderingIForeldrepengerListe/>" +
        "        </ns6:arbeidsforhold>" +
        "        <ns6:refusjon>" +
        "            <ns6:endringIRefusjonListe/>" +
        "        </ns6:refusjon>" +
        "        <ns6:sykepengerIArbeidsgiverperioden>" +
        "            <ns6:arbeidsgiverperiodeListe>" +
        perioder.stream().map { (fom, tom) ->
            "<ns6:arbeidsgiverperiode>" +
                "   <ns6:fom>" + DateTimeFormatter.ISO_DATE.format(fom) + "</ns6:fom>" +
                "   <ns6:tom>" + DateTimeFormatter.ISO_DATE.format(tom) + "</ns6:tom>" +
                "</ns6:arbeidsgiverperiode>"
        }.reduce("", BinaryOperator<String> { obj, str -> obj + str }) +
        "            </ns6:arbeidsgiverperiodeListe>" +
        "            <ns6:bruttoUtbetalt>2000</ns6:bruttoUtbetalt>" +
        "        </ns6:sykepengerIArbeidsgiverperioden>" +
        "        <ns6:opphoerAvNaturalytelseListe/>" +
        "        <ns6:gjenopptakelseNaturalytelseListe/>" +
        "        <ns6:avsendersystem>" +
        "            <ns6:systemnavn>AltinnPortal</ns6:systemnavn>" +
        "            <ns6:systemversjon>1.0</ns6:systemversjon>" +
        "        </ns6:avsendersystem>" +
        "        <ns6:pleiepengerPerioder/>" +
        "        <ns6:omsorgspenger>" +
        "            <ns6:fravaersPerioder/>" +
        "            <ns6:delvisFravaersListe/>" +
        "        </ns6:omsorgspenger>" +
        "    </ns6:Skjemainnhold>" +
        "</ns6:melding>"
}
