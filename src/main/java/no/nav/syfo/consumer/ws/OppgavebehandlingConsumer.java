package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Oppgave;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.LagreOppgaveOppgaveIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.LagreOppgaveOptimistiskLasing;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.OppgavebehandlingV3;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSEndreOppgave;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSLagreOppgaveRequest;
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

    public void oppdaterOppgavebeskrivelse(Oppgave oppgave, String beskrivelse) {
        try {
            oppgavebehandlingV3.lagreOppgave(new WSLagreOppgaveRequest()
                    .withEndreOppgave(new WSEndreOppgave()
                            .withOppgaveId(oppgave.getOppgaveId())
                            .withSaksnummer(oppgave.getGsakSaksid())
                            .withBeskrivelse(beskrivelse))
                    .withEndretAvEnhetId(9999)
            );
            log.info("Oppdatert oppgave: {} på sak: {}", oppgave.getOppgaveId(), oppgave.getGsakSaksid());
        } catch (LagreOppgaveOppgaveIkkeFunnet e) {
            log.error("Feil i oppgavebehandling. Oppgave ikke funnet.", e);
            throw new RuntimeException("Feil i oppgavebehandling. Oppgave ikke funnet.", e);
        } catch (LagreOppgaveOptimistiskLasing e) {
            log.error("Feil i oppgavebehandling. Optimistisk låsing.", e);
            throw new RuntimeException("Feil i oppgavebehandling. Optimistisk låsing.", e);
        }
    }

    public String opprettOppgave(String fnr, Oppgave oppgave) {
        try {
            String oppgaveId = oppgavebehandlingV3.opprettOppgave(new WSOpprettOppgaveRequest()
                    .withOpprettetAvEnhetId(9999)
                    .withOpprettOppgave(new WSOpprettOppgave()
                            .withBrukerId(fnr)
                            .withBrukertypeKode("PERSON")
                            // TODO: I diagrammet står det at typen skal være "Eletronisk inntektsmelding", hva skal den være?
                            .withOppgavetypeKode("INNT_SYK")
                            .withFagomradeKode("SYK")
                            .withUnderkategoriKode("SYK_SYK")
                            .withPrioritetKode("NORM_SYK")
                            .withBeskrivelse(oppgave.getBeskrivelse())
                            .withAktivFra(now())
                            .withAktivTil(oppgave.getAktivTil())
                            .withAnsvarligEnhetId(oppgave.getBehandlendeEnhetId())
                            .withDokumentId(oppgave.getJournalpostId())
                            .withMottattDato(now())
                            .withSaksnummer(oppgave.getGsakSaksid())
                            .withOppfolging("\nDu kan gi oss tilbakemelding på søknaden om sykepenger.\n" +
                                    "Gå til internettadresse: nav.no/digitalsykmelding/tilbakemelding")
                    )).getOppgaveId();
            log.info("Opprettet oppgave: {} på sak: {}", oppgaveId, oppgave.getGsakSaksid());
            return oppgaveId;
        } catch (RuntimeException e) {
            log.error("Klarte ikke å opprette oppgave. ", e);
            throw new RuntimeException(e);
        }
    }
}
