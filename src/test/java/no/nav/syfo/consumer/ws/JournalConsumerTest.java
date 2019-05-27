package no.nav.syfo.consumer.ws;

import no.nav.syfo.domain.InngaaendeJournal;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Periode;
import no.nav.tjeneste.virksomhet.journal.v2.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static no.nav.syfo.domain.InngaaendeJournal.MIDLERTIDIG;
import static no.nav.syfo.util.JAXBTest.getInntektsmelding;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class JournalConsumerTest {

    @Mock
    private JournalV2 journal;

    @InjectMocks
    private JournalConsumer journalConsumer;

    @Test
    public void hentInntektsmelding() throws Exception {
        when(journal.hentDokument(any())).thenReturn(new WSHentDokumentResponse().withDokument(getInntektsmelding().getBytes()));
        ArgumentCaptor<WSHentDokumentRequest> captor = ArgumentCaptor.forClass(WSHentDokumentRequest.class);

        Inntektsmelding inntektsmelding = journalConsumer.hentInntektsmelding("journalpostId", InngaaendeJournal.builder().dokumentId("dokumentId").status(MIDLERTIDIG).build());

        verify(journal).hentDokument(captor.capture());

        assertThat(inntektsmelding.getFnr()).isEqualTo("18018522868");
        assertThat(captor.getValue().getJournalpostId()).isEqualTo("journalpostId");
        assertThat(captor.getValue().getDokumentId()).isEqualTo("dokumentId");
    }

    @Test
    public void parserInntektsmeldingUtenPerioder() throws HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(journal.hentDokument(any())).thenReturn(new WSHentDokumentResponse().withDokument(inntektsmeldingArbeidsgiver(Collections.emptyList()).getBytes()));

        Inntektsmelding inntektsmelding = journalConsumer.hentInntektsmelding("jounralpostID", InngaaendeJournal.builder().build());

        assertThat(inntektsmelding.getArbeidsgiverperioder().isEmpty());
    }

    @Test
    public void parseInntektsmeldingV7() throws HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(journal.hentDokument(any())).thenReturn(new WSHentDokumentResponse().withDokument(inntektsmeldingArbeidsgiverPrivat().getBytes()));

        Inntektsmelding inntektsmelding = journalConsumer.hentInntektsmelding("journalpostId", InngaaendeJournal.builder().build());

        assertThat(inntektsmelding.getArbeidsgiverperioder().isEmpty()).isFalse();
        assertThat(inntektsmelding.getArbeidsgiverPrivat().isPresent()).isTrue();
    }

    @Test
    public void parseInntektsmelding0924() throws HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(journal.hentDokument(any())).thenReturn(new WSHentDokumentResponse().withDokument(inntektsmeldingArbeidsgiver(asList(Periode.builder().fom(LocalDate.of(2019,2,1)).tom(LocalDate.of(2019,2,16)).build())).getBytes()));

        Inntektsmelding inntektsmelding = journalConsumer.hentInntektsmelding("journalpostId", InngaaendeJournal.builder().build());

        assertThat(inntektsmelding.getArbeidsgiverOrgnummer().isPresent()).isTrue();
        assertThat(inntektsmelding.getArbeidsgiverPrivat().isPresent()).isFalse();
    }
    public static String inntektsmeldingArbeidsgiver(List<Periode> perioder) {
        return inntektsmeldingArbeidsgiver(perioder, "fnr");
    }

    public static String inntektsmeldingArbeidsgiver(List<Periode> perioder, String fnr) {
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
                perioder.stream().map(p ->
                                "<ns6:arbeidsgiverperiode>" +
                                "   <ns6:fom>"+DateTimeFormatter.ISO_DATE.format(p.getFom())+"</ns6:fom>" +
                                "   <ns6:tom>"+DateTimeFormatter.ISO_DATE.format(p.getTom())+"</ns6:tom>" +
                                "</ns6:arbeidsgiverperiode>").reduce("", String::concat) +
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
                "</ns6:melding>";
    }

    public static String inntektsmeldingArbeidsgiverPrivat() {
        return "<ns7:melding xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:seres=\"http://seres.no/xsd/forvaltningsdata\" xmlns:ns1=\"http://seres.no/xsd/NAV/Inntektsmelding_M/2017\" xmlns:ns2=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20171205\" xmlns:dfs=\"http://schemas.microsoft.com/office/infopath/2003/dataFormSolution\" xmlns:tns=\"http://www.altinn.no/services/ServiceEngine/ServiceMetaData/2009/10\" xmlns:q1=\"http://schemas.altinn.no/services/ServiceEngine/ServiceMetaData/2009/10\" xmlns:q2=\"http://schemas.altinn.no/serviceengine/formsengine/2009/10\" xmlns:ns3=\"http://www.altinn.no/services/2009/10\" xmlns:q3=\"http://www.altinn.no/services/common/fault/2009/10\" xmlns:ns4=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:ns5=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180618\" xmlns:ns6=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180924\" xmlns:my=\"http://schemas.microsoft.com/office/infopath/2003/myXSD/2017-10-18T12:15:13\" xmlns:xd=\"http://schemas.microsoft.com/office/infopath/2003\" xmlns:ns7=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20181211\">\n" +
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
                "</ns7:melding>";
    }
}
