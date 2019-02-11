package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.InngaaendeJournal;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.util.JAXB;
import no.nav.tjeneste.virksomhet.journal.v2.*;
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLInntektsmeldingM;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

@Component
@Slf4j
public class JournalConsumer {

    private JournalV2 journalV2;

    public JournalConsumer(JournalV2 journalV2) {
        this.journalV2 = journalV2;
    }

    public Inntektsmelding hentInntektsmelding(String journalpostId, InngaaendeJournal inngaaendeJournal) {
        WSHentDokumentRequest request = new WSHentDokumentRequest()
                .withJournalpostId(journalpostId)
                .withDokumentId(inngaaendeJournal.getDokumentId())
                .withVariantformat(new WSVariantformater().withValue("ORIGINAL"));

        try {
            final byte[] inntektsmeldingRAW = journalV2.hentDokument(request).getDokument();
            String inntektsmelding = new String(inntektsmeldingRAW);

            JAXBElement<Object> jaxbInntektsmelding = JAXB.unmarshalInntektsmelding(inntektsmelding);

            XMLInntektsmelding xmlInntektsmelding = jaxbInntektsmelding.getValue() instanceof XMLInntektsmeldingM
                    ? InntektsmeldingArbeidsgiver20180924Mapper.tilXMLInntektsmelding(jaxbInntektsmelding)
                    : InntektsmeldingArbeidsgiverPrivat20181211Mapper.tilXMLInntektsmelding(jaxbInntektsmelding);

            return Inntektsmelding.builder()
                    .fnr(xmlInntektsmelding.getArbeidstakerFnr())
                    .arbeidsgiverOrgnummer(xmlInntektsmelding.getVirksomhetsnummer())
                    .arbeidsgiverPrivat(xmlInntektsmelding.getArbeidsgiverPrivat())
                    .journalpostId(journalpostId)
                    .arbeidsforholdId(xmlInntektsmelding.getArbeidsforholdId())
                    .arsakTilInnsending(xmlInntektsmelding.getAarsakTilInnsending())
                    .status(inngaaendeJournal.getStatus())
                    .arbeidsgiverperioder(xmlInntektsmelding.getPerioder())
                    .build();
        } catch (HentDokumentSikkerhetsbegrensning e) {
            log.error("Feil ved henting av dokument: Sikkerhetsbegrensning!");
            throw new RuntimeException("Feil ved henting av dokument: Sikkerhetsbegrensning!", e);
        } catch (HentDokumentDokumentIkkeFunnet e) {
            log.error("Feil ved henting av dokument: Dokument ikke funnet!");
            throw new RuntimeException("Feil ved henting av journalpost: Dokument ikke funnet!", e);
        } catch (RuntimeException e) {
            log.error("Klarte ikke å hente inntektsmelding med journalpostId: {} og dokumentId: {}", journalpostId, inngaaendeJournal.getDokumentId(), e);
            throw new RuntimeException(e);
        }
    }
}
