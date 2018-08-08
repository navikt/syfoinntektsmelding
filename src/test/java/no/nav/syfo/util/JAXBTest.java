package no.nav.syfo.util;

import no.seres.xsd.nav.inntektsmelding_m._20180618.XMLInntektsmeldingM;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.assertj.core.api.Assertions.assertThat;

public class JAXBTest {
    public static String getInntektsmelding() {
        return "<ns2:melding xmlns:ns2=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180618\">\n" +
                "    <ns2:Skjemainnhold>\n" +
                "        <ns2:ytelse>Sykepenger</ns2:ytelse>\n" +
                "        <ns2:aarsakTilInnsending>Ny</ns2:aarsakTilInnsending>\n" +
                "        <ns2:arbeidsgiver>\n" +
                "            <ns2:virksomhetsnummer>910969900</ns2:virksomhetsnummer>\n" +
                "            <ns2:kontaktinformasjon>\n" +
                "                <ns2:kontaktinformasjonNavn>ismail Grøtte</ns2:kontaktinformasjonNavn>\n" +
                "                <ns2:telefonnummer>12312312312</ns2:telefonnummer>\n" +
                "            </ns2:kontaktinformasjon>\n" +
                "            <ns2:juridiskEnhet>910969900</ns2:juridiskEnhet>\n" +
                "        </ns2:arbeidsgiver>\n" +
                "        <ns2:arbeidstakerFnr>18018522868</ns2:arbeidstakerFnr>\n" +
                "        <ns2:naerRelasjon>false</ns2:naerRelasjon>\n" +
                "        <ns2:arbeidsforhold>\n" +
                "            <ns2:beregnetInntekt>\n" +
                "                <ns2:beloep>32500</ns2:beloep>\n" +
                "            </ns2:beregnetInntekt>\n" +
                "            <ns2:avtaltFerieListe>\n" +
                "                <ns2:avtaltFerie></ns2:avtaltFerie>\n" +
                "            </ns2:avtaltFerieListe>\n" +
                "            <ns2:utsettelseAvForeldrepengerListe/>\n" +
                "            <ns2:graderingIForeldrepengerListe/>\n" +
                "        </ns2:arbeidsforhold>\n" +
                "        <ns2:refusjon></ns2:refusjon>\n" +
                "        <ns2:sykepengerIArbeidsgiverperioden>\n" +
                "            <ns2:arbeidsgiverperiodeListe>\n" +
                "                <ns2:arbeidsgiverperiode>\n" +
                "                    <ns2:fom>2018-03-01</ns2:fom>\n" +
                "                    <ns2:tom>2018-03-16</ns2:tom>\n" +
                "                </ns2:arbeidsgiverperiode>\n" +
                "            </ns2:arbeidsgiverperiodeListe>\n" +
                "            <ns2:bruttoUtbetalt>18000</ns2:bruttoUtbetalt>\n" +
                "        </ns2:sykepengerIArbeidsgiverperioden>\n" +
                "        <ns2:opphoerAvNaturalytelseListe/>\n" +
                "        <ns2:gjenopptakelseNaturalytelseListe/>\n" +
                "        <ns2:avsendersystem>\n" +
                "            <ns2:systemnavn>AltinnPortal</ns2:systemnavn>\n" +
                "            <ns2:systemversjon>1.0</ns2:systemversjon>\n" +
                "        </ns2:avsendersystem>\n" +
                "    </ns2:Skjemainnhold>\n" +
                "</ns2:melding>";
    }

    @Test
    public void unmarshalInntektsmelding() {
        JAXBElement<XMLInntektsmeldingM> inntektsmeldingM = JAXB.unmarshalInntektsmelding(getInntektsmelding());
        assertThat(inntektsmeldingM.getValue().getSkjemainnhold().getArbeidsgiver().getVirksomhetsnummer()).isEqualTo("910969900");
    }

}