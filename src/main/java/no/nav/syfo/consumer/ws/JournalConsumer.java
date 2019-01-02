package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.InngaaendeJournal;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.util.JAXB;
import no.nav.tjeneste.virksomhet.journal.v2.*;
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLInntektsmeldingM;
import no.seres.xsd.nav.inntektsmelding_m._20180924.XMLSkjemainnhold;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.Optional;

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
            log.info(inntektsmelding);
            JAXBElement<XMLInntektsmeldingM> inntektsmeldingM = JAXB.unmarshalInntektsmelding(inntektsmelding);
            final XMLSkjemainnhold skjemainnhold = inntektsmeldingM.getValue().getSkjemainnhold();
            String arbeidsforholdId =
                    Optional.ofNullable(skjemainnhold.getArbeidsforhold().getValue().getArbeidsforholdId())
                            .map(JAXBElement::getValue)
                            .orElse(null);

            return Inntektsmelding.builder()
                    .fnr(skjemainnhold.getArbeidstakerFnr())
                    .arbeidsgiverOrgnummer(skjemainnhold.getArbeidsgiver().getVirksomhetsnummer())
                    .journalpostId(journalpostId)
                    .arbeidsforholdId(arbeidsforholdId)
                    .arsakTilInnsending(skjemainnhold.getAarsakTilInnsending())
                    .status(inngaaendeJournal.getStatus())
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
