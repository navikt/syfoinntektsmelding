package no.nav.syfo.syfoinnteksmelding.util

import no.nav.syfo.util.JAXB.unmarshalInntektsmelding
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLInntektsmeldingM
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import javax.xml.bind.JAXBElement

class JAXBTest {
    @Test
    fun unmarshalInntektsmelding() {
        val inntektsmeldingM = unmarshalInntektsmelding<JAXBElement<XMLInntektsmeldingM>>(inntektsmelding)
        Assertions.assertThat(inntektsmeldingM.value.skjemainnhold.arbeidsgiver.virksomhetsnummer)
            .isEqualTo("910969900")
    }

    companion object {
        val inntektsmelding: String
            get() = """<ns2:melding xmlns:ns2="http://seres.no/xsd/NAV/Inntektsmelding_M/20180924">
    <ns2:Skjemainnhold>
        <ns2:ytelse>Sykepenger</ns2:ytelse>
        <ns2:aarsakTilInnsending>Ny</ns2:aarsakTilInnsending>
        <ns2:arbeidsgiver>
            <ns2:virksomhetsnummer>910969900</ns2:virksomhetsnummer>
            <ns2:kontaktinformasjon>
                <ns2:kontaktinformasjonNavn>ismail Grøtte</ns2:kontaktinformasjonNavn>
                <ns2:telefonnummer>12312312312</ns2:telefonnummer>
            </ns2:kontaktinformasjon>
            <ns2:juridiskEnhet>910969900</ns2:juridiskEnhet>
        </ns2:arbeidsgiver>
        <ns2:arbeidstakerFnr>18018522868</ns2:arbeidstakerFnr>
        <ns2:naerRelasjon>false</ns2:naerRelasjon>
        <ns2:arbeidsforhold>
            <ns2:beregnetInntekt>
                <ns2:beloep>32500</ns2:beloep>
            </ns2:beregnetInntekt>
            <ns2:avtaltFerieListe>
                <ns2:avtaltFerie></ns2:avtaltFerie>
            </ns2:avtaltFerieListe>
            <ns2:utsettelseAvForeldrepengerListe/>
            <ns2:graderingIForeldrepengerListe/>
            <ns2:foersteFravaersdag>2019-02-01</ns2:foersteFravaersdag>        </ns2:arbeidsforhold>
        <ns2:refusjon></ns2:refusjon>
        <ns2:sykepengerIArbeidsgiverperioden>
            <ns2:arbeidsgiverperiodeListe>
                <ns2:arbeidsgiverperiode>
                    <ns2:fom>2018-03-01</ns2:fom>
                    <ns2:tom>2018-03-16</ns2:tom>
                </ns2:arbeidsgiverperiode>
            </ns2:arbeidsgiverperiodeListe>
            <ns2:bruttoUtbetalt>18000</ns2:bruttoUtbetalt>
        </ns2:sykepengerIArbeidsgiverperioden>
        <ns2:opphoerAvNaturalytelseListe/>
        <ns2:gjenopptakelseNaturalytelseListe/>
        <ns2:avsendersystem>
            <ns2:systemnavn>AltinnPortal</ns2:systemnavn>
            <ns2:systemversjon>1.0</ns2:systemversjon>
        </ns2:avsendersystem>
    </ns2:Skjemainnhold>
</ns2:melding>"""
    }
}
