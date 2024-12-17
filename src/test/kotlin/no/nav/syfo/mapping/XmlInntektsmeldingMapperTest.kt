package no.nav.syfo.mapping

import io.mockk.every
import io.mockk.mockk
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.domain.Periode
import no.nav.syfo.util.getAktørid
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class XmlInntektsmeldingMapperTest {
    val mottattDato = LocalDateTime.of(2021, 7, 18, 16, 0, 0)
    val journalpostId = "journalpostId"
    val arkivReferanse = "ar-123"
    var pdlClient = mockk<PdlClient>(relaxed = true)

    @Before
    fun all() {
        every {
            pdlClient.getAktørid(any())
        } returns "aktør-123"
    }

    @Test
    fun map20180924() {
        val bytes: ByteArray = inntektsmelding.toByteArray()
        val im =
            XmlInntektsmeldingMapper().mapInntektsmelding(
                bytes,
                pdlClient,
                mottattDato,
                journalpostId,
                JournalStatus.MOTTATT,
                arkivReferanse,
            )
        Assertions.assertThat(im.fnr).isEqualTo("18018522868")
        Assertions.assertThat(im.journalpostId).isEqualTo("journalpostId")
        Assertions.assertThat(im.arbeidsgiverperioder.size).isEqualTo(1)
        Assertions.assertThat(im.begrunnelseRedusert).isEqualTo("")
        Assertions.assertThat(im.bruttoUtbetalt).isEqualTo(BigDecimal(18000))
    }

    @Test
    fun map20180924_perioder() {
        val p1 = Periode(LocalDate.of(2021, 6, 1), LocalDate.of(2021, 6, 17))
        val p2 = Periode(LocalDate.of(2021, 7, 1), LocalDate.of(2021, 7, 17))
        val bytes: ByteArray = inntektsmeldingArbeidsgiver(listOf(p1, p2), "fnr-2").toByteArray()
        val im =
            XmlInntektsmeldingMapper().mapInntektsmelding(
                bytes,
                pdlClient,
                mottattDato,
                journalpostId,
                JournalStatus.MOTTATT,
                arkivReferanse,
            )
        Assertions.assertThat(im.fnr).isEqualTo("fnr-2")
        Assertions.assertThat(im.journalpostId).isEqualTo("journalpostId")
        Assertions.assertThat(im.arbeidsgiverperioder.size).isEqualTo(2)
    }

    @Test
    fun map20181211() {
        val bytes: ByteArray = inntektsmeldingArbeidsgiverPrivat().toByteArray()
        val im =
            XmlInntektsmeldingMapper().mapInntektsmelding(
                bytes,
                pdlClient,
                mottattDato,
                journalpostId,
                JournalStatus.MOTTATT,
                arkivReferanse,
            )
        Assertions.assertThat(im.fnr).isEqualTo("fnr")
        Assertions.assertThat(im.journalpostId).isEqualTo("journalpostId")
        Assertions.assertThat(im.arbeidsgiverperioder.size).isEqualTo(1)
        Assertions.assertThat(im.begrunnelseRedusert).isEqualTo("")
        Assertions.assertThat(im.bruttoUtbetalt).isEqualTo(BigDecimal(9889))
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

        @JvmOverloads
        fun inntektsmeldingArbeidsgiver(
            perioder: List<Periode>,
            fnr: String = "fnr",
        ): String =
            "<ns6:melding xmlns:ns6=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180924\">" +
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
                perioder
                    .stream()
                    .map { (fom, tom) ->
                        "<ns6:arbeidsgiverperiode>" +
                            "   <ns6:fom>" + DateTimeFormatter.ISO_DATE.format(fom) + "</ns6:fom>" +
                            "   <ns6:tom>" + DateTimeFormatter.ISO_DATE.format(tom) + "</ns6:tom>" +
                            "</ns6:arbeidsgiverperiode>"
                    }.reduce("") { obj, str -> obj + str } +
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

        fun inntektsmeldingArbeidsgiverPrivat(): String =
            "<ns7:melding xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:seres=\"http://seres.no/xsd/forvaltningsdata\" xmlns:ns1=\"http://seres.no/xsd/NAV/Inntektsmelding_M/2017\" xmlns:ns2=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20171205\" xmlns:dfs=\"http://schemas.microsoft.com/office/infopath/2003/dataFormSolution\" xmlns:tns=\"http://www.altinn.no/services/ServiceEngine/ServiceMetaData/2009/10\" xmlns:q1=\"http://schemas.altinn.no/services/ServiceEngine/ServiceMetaData/2009/10\" xmlns:q2=\"http://schemas.altinn.no/serviceengine/formsengine/2009/10\" xmlns:ns3=\"http://www.altinn.no/services/2009/10\" xmlns:q3=\"http://www.altinn.no/services/common/fault/2009/10\" xmlns:ns4=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:ns5=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180618\" xmlns:ns6=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180924\" xmlns:my=\"http://schemas.microsoft.com/office/infopath/2003/myXSD/2017-10-18T12:15:13\" xmlns:xd=\"http://schemas.microsoft.com/office/infopath/2003\" xmlns:ns7=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20181211\">\n" +
                "<ns7:Skjemainnhold>" +
                "<ns7:ytelse>Sykepenger</ns7:ytelse>" +
                "<ns7:aarsakTilInnsending>Ny</ns7:aarsakTilInnsending>" +
                "<ns7:arbeidsgiverPrivat>" +
                "<ns7:arbeidsgiverFnr>arbeidsgiverPrivat</ns7:arbeidsgiverFnr>" +
                "<ns7:kontaktinformasjon>" +
                "<ns7:kontaktinformasjonNavn>Postman Pat</ns7:kontaktinformasjonNavn>" +
                "<ns7:telefonnummer>81549300</ns7:telefonnummer>" +
                "</ns7:kontaktinformasjon>" +
                "</ns7:arbeidsgiverPrivat>" +
                "<ns7:arbeidstakerFnr>fnr</ns7:arbeidstakerFnr>" +
                "<ns7:naerRelasjon>false</ns7:naerRelasjon>" +
                "<ns7:arbeidsforhold>" +
                "<ns7:foersteFravaersdag>2019-02-01</ns7:foersteFravaersdag>" +
                "<ns7:beregnetInntekt>" +
                "<ns7:beloep>20000</ns7:beloep>" +
                "</ns7:beregnetInntekt>" +
                "<ns7:avtaltFerieListe/>" +
                "<ns7:utsettelseAvForeldrepengerListe/>" +
                "<ns7:graderingIForeldrepengerListe/>" +
                "</ns7:arbeidsforhold>" +
                "<ns7:refusjon>" +
                "<ns7:endringIRefusjonListe/>" +
                "</ns7:refusjon>" +
                "<ns7:sykepengerIArbeidsgiverperioden>" +
                "<ns7:arbeidsgiverperiodeListe>" +
                "<ns7:arbeidsgiverperiode>" +
                "<ns7:fom>2018-12-01</ns7:fom>" +
                "<ns7:tom>2018-12-16</ns7:tom>" +
                "</ns7:arbeidsgiverperiode>" +
                "</ns7:arbeidsgiverperiodeListe>" +
                "<ns7:bruttoUtbetalt>9889</ns7:bruttoUtbetalt>" +
                "</ns7:sykepengerIArbeidsgiverperioden>" +
                "<ns7:opphoerAvNaturalytelseListe/>" +
                "<ns7:gjenopptakelseNaturalytelseListe/>" +
                "<ns7:avsendersystem>" +
                "<ns7:systemnavn>AltinnPortal</ns7:systemnavn>" +
                "<ns7:systemversjon>1.0</ns7:systemversjon>" +
                "</ns7:avsendersystem>" +
                "<ns7:pleiepengerPerioder/>" +
                "<ns7:omsorgspenger>" +
                "<ns7:fravaersPerioder/>" +
                "<ns7:delvisFravaersListe/>" +
                "</ns7:omsorgspenger>" +
                "</ns7:Skjemainnhold>" +
                "</ns7:melding>"
    }
}
