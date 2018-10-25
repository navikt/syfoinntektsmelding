package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Oppgave;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.OppgavebehandlingV3;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgave;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgaveRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.time.LocalDate.now;

@Component
@Slf4j
public class OppgavebehandlingConsumer {
    private final OppgavebehandlingV3 oppgavebehandlingV3;

    @Inject
    public OppgavebehandlingConsumer(OppgavebehandlingV3 oppgavebehandlingV3) {
        this.oppgavebehandlingV3 = oppgavebehandlingV3;
    }

    public void opprettOppgave(String fnr, Oppgave oppgave) {
        try {
            oppgavebehandlingV3.opprettOppgave(new WSOpprettOppgaveRequest()
                    .withOpprettetAvEnhetId(9999)
                    .withOpprettOppgave(new WSOpprettOppgave()
                            .withBrukerId(fnr)
                            .withBrukertypeKode("PERSON")
                            .withOppgavetypeKode("INNT_SYK")
                            .withFagomradeKode("SYK")
                            .withUnderkategoriKode("SYK_SYK")
                            .withPrioritetKode("NORM_SYK")
                            .withBeskrivelse(oppgave.getBeskrivelse())
                            .withAktivFra(now())
                            .withAktivTil(oppgave.getAktivTil())
                            .withAnsvarligEnhetId(oppgave.getAnsvarligEnhetId())
                            .withDokumentId(oppgave.getDokumentId())
                            .withMottattDato(now())
                            .withSaksnummer(oppgave.getSaksnummer())
                            .withOppfolging("\nDu kan gi oss tilbakemelding på søknaden om sykepenger.\n" +
                                    "Gå til internettadresse: nav.no/digitalsykmelding/tilbakemelding")
                    )).getOppgaveId();
        } catch (RuntimeException e) {
            log.error("Klarte ikke å opprette oppgave. ", e);
            throw new RuntimeException(e);
        }
    }
}
