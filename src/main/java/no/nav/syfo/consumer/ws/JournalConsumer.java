package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.InngaaendeJournalpost;
import no.nav.syfo.domain.SyfoException;
import no.nav.syfo.util.JAXB;
import no.nav.tjeneste.virksomhet.journal.v3.*;
import no.seres.xsd.nav.inntektsmelding_m._20171205.InntektsmeldingM;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@Slf4j
public class JournalConsumer {

    private JournalV3 journalV3;

    public JournalConsumer(JournalV3 journalV3) {
        this.journalV3 = journalV3;
    }

    public InntektsmeldingM hentXmlDokument(String journalpostId, String dokumentId) {
        WSHentDokumentRequest request = new WSHentDokumentRequest()
                .withJournalpostId(journalpostId)
                .withDokumentId(dokumentId)
                .withVariantformat(new WSVariantformater().withValue("ARKIV"));

        try {
            final byte[] inntektsmeldingRAW = journalV3.hentDokument(request).getDokument();

            log.info("InntektsmeldingRAW: {}", inntektsmeldingRAW);

            final Base64.Decoder decoder = Base64.getDecoder();
            final String inntektsmelding = new String(decoder.decode(inntektsmeldingRAW));
            InntektsmeldingM inntektsmeldingM = JAXB.unmarshalInntektsmelding(inntektsmelding);

            log.info("Inntektsmelding med arbeidsgiver: {}", inntektsmeldingM.getSkjemainnhold().getArbeidsgiver().getJuridiskEnhet());
            return inntektsmeldingM;

        } catch (HentDokumentSikkerhetsbegrensning e) {
            log.error("Feil ved henting av dokument: Sikkerhetsbegrensning!");
            throw new SyfoException("Feil ved henting av dokument: Sikkerhetsbegrensning!", e);
        } catch (HentDokumentDokumentIkkeFunnet e) {
            log.error("Feil ved henting av dokument: Dokument ikke funnet!");
            throw new SyfoException("Feil ved henting av journalpost: Dokument ikke funnet!", e);
        } catch (HentDokumentJournalpostIkkeFunnet e) {
            log.error("Feil ved henting av dokument: Journalpost ikke funnet!");
            throw new SyfoException("Feil ved henting av dokument: Journalpost ikke funnet!", e);
        }

    }
}
