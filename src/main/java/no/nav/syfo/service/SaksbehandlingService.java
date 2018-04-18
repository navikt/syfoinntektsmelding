package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.ArbeidsfordelingConsumer;
import no.nav.syfo.consumer.ws.PersonConsumer;
import no.nav.tjeneste.virksomhet.behandlesak.v1.BehandleSakV1;
import no.nav.tjeneste.virksomhet.behandlesak.v1.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandlesak.v1.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.WSOpprettSakRequest;
import no.nav.tjeneste.virksomhet.oppgave.v3.HentOppgaveOppgaveIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppgave.v3.OppgaveV3;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.WSOppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.WSHentOppgaveRequest;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.LagreOppgaveOppgaveIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.LagreOppgaveOptimistiskLasing;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.OppgavebehandlingV3;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSEndreOppgave;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSLagreOppgaveRequest;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgave;
import no.nav.tjeneste.virksomhet.oppgavebehandling.v3.meldinger.WSOpprettOppgaveRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;

import static java.time.LocalDate.now;
import static org.jvnet.jaxb2_commons.lang.StringUtils.isEmpty;

@Component
@Slf4j
public class SaksbehandlingService {
    private final BehandleSakV1 behandleSakV1;
    private final OppgaveV3 oppgaveV3;
    private final OppgavebehandlingV3 oppgavebehandlingV3;
    private final PersonConsumer personConsumer;
    private final ArbeidsfordelingConsumer arbeidsfordelingConsumer;

    @Inject
    public SaksbehandlingService(
            BehandleSakV1 behandleSakV1,
            OppgaveV3 oppgaveV3,
            OppgavebehandlingV3 oppgavebehandlingV3,
            ArbeidsfordelingConsumer arbeidsfordelingConsumer,
            PersonConsumer personConsumer
    ) {
        this.behandleSakV1 = behandleSakV1;
        this.oppgaveV3 = oppgaveV3;
        this.oppgavebehandlingV3 = oppgavebehandlingV3;
        this.personConsumer = personConsumer;
        this.arbeidsfordelingConsumer = arbeidsfordelingConsumer;
    }

    public String opprettSak(String fnr) {
        try {
            String sakId = behandleSakV1.opprettSak(new WSOpprettSakRequest()
                    .withSak(new WSSak()
                            .withSakstype(new WSSakstyper().withValue("GEN"))
                            .withFagomraade(new WSFagomraader().withValue("SYK"))
                            .withFagsystem(new WSFagsystemer().withValue("FS22"))
                            .withGjelderBrukerListe(new WSPerson().withIdent(fnr))
                    )
            ).getSakId();
            log.info("Opprettet sak i GSAK: {}", sakId);
            return sakId;
        } catch (OpprettSakSakEksistererAllerede e) {
            log.error("Sak finnes allerede", e);
            throw new RuntimeException("Sak finnes allerede", e);

        } catch (OpprettSakUgyldigInput e) {
            log.error("Ugyldig input", e);
            throw new RuntimeException("Ugyldid input i sak", e);
        }
    }

    public WSOppgave finnOppgave(String oppgaveId) {
        if (isEmpty(oppgaveId)) {
            return null;
        }

        try {
            return oppgaveV3.hentOppgave(new WSHentOppgaveRequest().withOppgaveId(oppgaveId)).getOppgave();
        } catch (HentOppgaveOppgaveIkkeFunnet e) {
            log.warn("Fant ikke oppgave med id " + oppgaveId);
            return null;
        }
    }

    public String opprettOppgave(String fnr, String beskrivelse, String gsakSaksid, String journalpostId, LocalDate aktivTil) {
        String geografiskTilknytning = personConsumer.hentGeografiskTilknytning(fnr);
        String behandlendeEnhet = arbeidsfordelingConsumer.finnBehandlendeEnhet(geografiskTilknytning);

        String oppgaveId = oppgavebehandlingV3.opprettOppgave(new WSOpprettOppgaveRequest()
                .withOpprettetAvEnhetId(9999)
                .withOpprettOppgave(new WSOpprettOppgave()
                        .withBrukerId(fnr)
                        .withBrukertypeKode("PERSON")
                        .withOppgavetypeKode("INNT_SYK")
                        .withFagomradeKode("SYK")
                        .withUnderkategoriKode("SYK_SYK")
                        .withPrioritetKode("NORM_SYK")
                        .withBeskrivelse(beskrivelse)
                        .withAktivFra(now())
                        .withAktivTil(aktivTil)
                        .withAnsvarligEnhetId(behandlendeEnhet)
                        .withDokumentId(journalpostId)
                        .withMottattDato(now())
                        .withSaksnummer(gsakSaksid)
                        .withOppfolging("\nDu kan gi oss tilbakemelding på søknaden om sykepenger.\n" +
                                "Gå til internettadresse: nav.no/digitalsykmelding/tilbakemelding")
                )).getOppgaveId();
        log.info("Opprettet oppgave: {} på sak: {}", oppgaveId, gsakSaksid);
        return oppgaveId;
    }

    public String oppdaterOppgavebeskrivelse(WSOppgave oppgave, String beskrivelse) {
        try {
            oppgavebehandlingV3.lagreOppgave(new WSLagreOppgaveRequest()
                    .withEndreOppgave(new WSEndreOppgave().withBeskrivelse(beskrivelse))
                    .withEndretAvEnhetId(9999)
            );
            log.info("Oppdatert oppgave: {} på sak: {}", oppgave.getOppgaveId(), oppgave.getSaksnummer());
            return oppgave.getOppgaveId();
        } catch (LagreOppgaveOppgaveIkkeFunnet e) {
            log.error("Feil i oppgavebehandling. Oppgave ikke funnet.", e);
            throw new RuntimeException("Feil i oppgavebehandling. Oppgave ikke funnet.", e);
        } catch (LagreOppgaveOptimistiskLasing e) {
            log.error("Feil i oppgavebehandling. Optimistisk låsing.", e);
            throw new RuntimeException("Feil i oppgavebehandling. Optimistisk låsing.", e);
        }
    }

}
