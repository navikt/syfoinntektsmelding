package no.nav.syfo.consumer.ws;

import no.nav.syfo.domain.InngaaendeJournal;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.tjeneste.virksomhet.journal.v2.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
        when(journal.hentDokument(any())).thenReturn(new WSHentDokumentResponse().withDokument(inntektsmeldingUtenPerioder().getBytes()));

        Inntektsmelding inntektsmelding = journalConsumer.hentInntektsmelding("jounralpostID", InngaaendeJournal.builder().build());

        assertThat(inntektsmelding.getArbeidsgiverperioder().isEmpty());
    }

    @Test
    public void parseInntektsmeldingV7() throws HentDokumentSikkerhetsbegrensning, HentDokumentDokumentIkkeFunnet {
        when(journal.hentDokument(any())).thenReturn(new WSHentDokumentResponse().withDokument(inntektsmeldingV7().getBytes()));

        Inntektsmelding inntektsmelding = journalConsumer.hentInntektsmelding("journalpostId", InngaaendeJournal.builder().build());

        assertThat(inntektsmelding.getArbeidsgiverperioder().isEmpty()).isFalse();
        assertThat(inntektsmelding.getOrgnummerPrivatperson().isPresent()).isTrue();
    }

    private String inntektsmeldingUtenPerioder() {
        return "<ns6:melding xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:seres=\"http://seres.no/xsd/forvaltningsdata\" xmlns:ns1=\"http://seres.no/xsd/NAV/Inntektsmelding_M/2017\" xmlns:ns2=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20171205\" xmlns:dfs=\"http://schemas.microsoft.com/office/infopath/2003/dataFormSolution\" xmlns:tns=\"http://www.altinn.no/services/ServiceEngine/ServiceMetaData/2009/10\" xmlns:q1=\"http://schemas.altinn.no/services/ServiceEngine/ServiceMetaData/2009/10\" xmlns:q2=\"http://schemas.altinn.no/serviceengine/formsengine/2009/10\" xmlns:ns3=\"http://www.altinn.no/services/2009/10\" xmlns:q3=\"http://www.altinn.no/services/common/fault/2009/10\" xmlns:ns4=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:ns5=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180618\" xmlns:ns6=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180924\" xmlns:my=\"http://schemas.microsoft.com/office/infopath/2003/myXSD/2017-10-18T12:15:13\" xmlns:xd=\"http://schemas.microsoft.com/office/infopath/2003\"><ns6:Skjemainnhold><ns6:ytelse>Sykepenger</ns6:ytelse><ns6:aarsakTilInnsending>Ny</ns6:aarsakTilInnsending><ns6:arbeidsgiver><ns6:virksomhetsnummer>orgnummer</ns6:virksomhetsnummer><ns6:kontaktinformasjon><ns6:kontaktinformasjonNavn>Are Vassdal</ns6:kontaktinformasjonNavn><ns6:telefonnummer>11111111</ns6:telefonnummer></ns6:kontaktinformasjon></ns6:arbeidsgiver><ns6:arbeidstakerFnr>12345678910</ns6:arbeidstakerFnr><ns6:naerRelasjon>false</ns6:naerRelasjon><ns6:arbeidsforhold><ns6:arbeidsforholdId>111</ns6:arbeidsforholdId><ns6:foersteFravaersdag>2019-01-14</ns6:foersteFravaersdag><ns6:beregnetInntekt><ns6:beloep>11222</ns6:beloep></ns6:beregnetInntekt><ns6:avtaltFerieListe /><ns6:utsettelseAvForeldrepengerListe /><ns6:graderingIForeldrepengerListe /></ns6:arbeidsforhold><ns6:refusjon><ns6:endringIRefusjonListe /></ns6:refusjon><ns6:sykepengerIArbeidsgiverperioden><ns6:arbeidsgiverperiodeListe><ns6:arbeidsgiverperiode></ns6:arbeidsgiverperiode></ns6:arbeidsgiverperiodeListe><ns6:bruttoUtbetalt>0</ns6:bruttoUtbetalt><ns6:begrunnelseForReduksjonEllerIkkeUtbetalt>ManglerOpptjening</ns6:begrunnelseForReduksjonEllerIkkeUtbetalt></ns6:sykepengerIArbeidsgiverperioden><ns6:opphoerAvNaturalytelseListe /><ns6:gjenopptakelseNaturalytelseListe /><ns6:avsendersystem><ns6:systemnavn>AltinnPortal</ns6:systemnavn><ns6:systemversjon>1.0</ns6:systemversjon></ns6:avsendersystem><ns6:pleiepengerPerioder /><ns6:omsorgspenger><ns6:fravaersPerioder /><ns6:delvisFravaersListe /></ns6:omsorgspenger></ns6:Skjemainnhold></ns6:melding>";
    }

    private String inntektsmeldingV7() {
        return "<ns7:melding xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:seres=\"http://seres.no/xsd/forvaltningsdata\" xmlns:ns1=\"http://seres.no/xsd/NAV/Inntektsmelding_M/2017\" xmlns:ns2=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20171205\" xmlns:dfs=\"http://schemas.microsoft.com/office/infopath/2003/dataFormSolution\" xmlns:tns=\"http://www.altinn.no/services/ServiceEngine/ServiceMetaData/2009/10\" xmlns:q1=\"http://schemas.altinn.no/services/ServiceEngine/ServiceMetaData/2009/10\" xmlns:q2=\"http://schemas.altinn.no/serviceengine/formsengine/2009/10\" xmlns:ns3=\"http://www.altinn.no/services/2009/10\" xmlns:q3=\"http://www.altinn.no/services/common/fault/2009/10\" xmlns:ns4=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:ns5=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180618\" xmlns:ns6=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20180924\" xmlns:my=\"http://schemas.microsoft.com/office/infopath/2003/myXSD/2017-10-18T12:15:13\" xmlns:xd=\"http://schemas.microsoft.com/office/infopath/2003\" xmlns:ns7=\"http://seres.no/xsd/NAV/Inntektsmelding_M/20181211\">\n" +
                "\t<ns7:Skjemainnhold>\n" +
                "\t\t<ns7:ytelse>Sykepenger</ns7:ytelse>\n" +
                "\t\t<ns7:aarsakTilInnsending>Ny</ns7:aarsakTilInnsending>\n" +
                "\t\t<ns7:arbeidsgiverPrivat>\n" +
                "\t\t\t<ns7:arbeidsgiverFnr>agFnr</ns7:arbeidsgiverFnr>\n" +
                "\t\t\t<ns7:kontaktinformasjon>\n" +
                "\t\t\t\t<ns7:kontaktinformasjonNavn>Postman Pat</ns7:kontaktinformasjonNavn>\n" +
                "\t\t\t\t<ns7:telefonnummer>81549300</ns7:telefonnummer>\n" +
                "\t\t\t</ns7:kontaktinformasjon>\n" +
                "\t\t</ns7:arbeidsgiverPrivat>\n" +
                "\t\t<ns7:arbeidstakerFnr>arbeidstakerFnr</ns7:arbeidstakerFnr>\n" +
                "\t\t<ns7:naerRelasjon>false</ns7:naerRelasjon>\n" +
                "\t\t<ns7:arbeidsforhold>\n" +
                "\t\t\t<ns7:beregnetInntekt>\n" +
                "\t\t\t\t<ns7:beloep>20000</ns7:beloep>\n" +
                "\t\t\t</ns7:beregnetInntekt>\n" +
                "\t\t\t<ns7:avtaltFerieListe/>\n" +
                "\t\t\t<ns7:utsettelseAvForeldrepengerListe/>\n" +
                "\t\t\t<ns7:graderingIForeldrepengerListe/>\n" +
                "\t\t</ns7:arbeidsforhold>\n" +
                "\t\t<ns7:refusjon>\n" +
                "\t\t\t<ns7:endringIRefusjonListe/>\n" +
                "\t\t</ns7:refusjon>\n" +
                "\t\t<ns7:sykepengerIArbeidsgiverperioden>\n" +
                "\t\t\t<ns7:arbeidsgiverperiodeListe>\n" +
                "\t\t\t\t<ns7:arbeidsgiverperiode>\n" +
                "\t\t\t\t\t<ns7:fom>2018-12-01</ns7:fom>\n" +
                "\t\t\t\t\t<ns7:tom>2018-12-16</ns7:tom>\n" +
                "\t\t\t\t</ns7:arbeidsgiverperiode>\n" +
                "\t\t\t</ns7:arbeidsgiverperiodeListe>\n" +
                "\t\t\t<ns7:bruttoUtbetalt>9889</ns7:bruttoUtbetalt>\n" +
                "\t\t</ns7:sykepengerIArbeidsgiverperioden>\n" +
                "\t\t<ns7:opphoerAvNaturalytelseListe/>\n" +
                "\t\t<ns7:gjenopptakelseNaturalytelseListe/>\n" +
                "\t\t<ns7:avsendersystem>\n" +
                "\t\t\t<ns7:systemnavn>AltinnPortal</ns7:systemnavn>\n" +
                "\t\t\t<ns7:systemversjon>1.0</ns7:systemversjon>\n" +
                "\t\t</ns7:avsendersystem>\n" +
                "\t\t<ns7:pleiepengerPerioder/>\n" +
                "\t\t<ns7:omsorgspenger>\n" +
                "\t\t\t<ns7:fravaersPerioder/>\n" +
                "\t\t\t<ns7:delvisFravaersListe/>\n" +
                "\t\t</ns7:omsorgspenger>\n" +
                "\t</ns7:Skjemainnhold>\n" +
                "</ns7:melding>";
    }

}
