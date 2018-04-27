package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.SyfoException;
import no.nav.syfo.util.JAXB;
import no.nav.tjeneste.virksomhet.journal.v3.*;
import no.seres.xsd.nav.inntektsmelding_m._20171205.InntektsmeldingM;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;

@Component
@Slf4j
public class JournalConsumer {

    private JournalV3 journalV3;

    public JournalConsumer(JournalV3 journalV3) {
        this.journalV3 = journalV3;
    }

    public Inntektsmelding hentInntektsmelding(String journalpostId, String dokumentId) {
        WSHentDokumentRequest request = new WSHentDokumentRequest()
                .withJournalpostId(journalpostId)
                .withDokumentId(dokumentId)
                .withVariantformat(new WSVariantformater().withValue("ORIGINAL"));

        try {
            final byte[] inntektsmeldingRAW = journalV3.hentDokument(request).getDokument();
            String inntektsmelding = new String(inntektsmeldingRAW);

            JAXBElement<InntektsmeldingM> inntektsmeldingM = JAXB.unmarshalInntektsmelding(inntektsmelding);

            log.info("Inntektsmelding med arbeidsgiver: {}", inntektsmeldingM.getValue().getSkjemainnhold().getArbeidsgiver().getJuridiskEnhet());
            return Inntektsmelding.builder()
                    .fnr(inntektsmeldingM.getValue().getSkjemainnhold().getArbeidstakerFnr())
                    .build();
        } catch (HentDokumentSikkerhetsbegrensning e) {
            log.error("Feil ved henting av dokument: Sikkerhetsbegrensning!");
            throw new SyfoException("Feil ved henting av dokument: Sikkerhetsbegrensning!", e);
        } catch (HentDokumentDokumentIkkeFunnet e) {
            log.error("Feil ved henting av dokument: Dokument ikke funnet!");
            throw new SyfoException("Feil ved henting av journalpost: Dokument ikke funnet!", e);
        } catch (HentDokumentJournalpostIkkeFunnet e) {
            log.error("Feil ved henting av dokument: Journalpost ikke funnet!");
            throw new SyfoException("Feil ved henting av dokument: Journalpost ikke funnet!", e);
        } catch (RuntimeException e) {
            log.error("Klarte ikke å hente inntekrsmelding med journalpostId: {} og dokumentId: {}", journalpostId, dokumentId, e);
            throw new RuntimeException(e);
        }
    }
}
