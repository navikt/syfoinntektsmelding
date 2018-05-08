package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.BehandleSakConsumer;
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer;
import no.nav.syfo.consumer.ws.OppgaveConsumer;
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.repository.SykepengesoknadDAO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

@Component
@Slf4j
public class SaksbehandlingService {
    private final OppgavebehandlingConsumer oppgavebehandlingConsumer;
    private final BehandlendeEnhetConsumer behandlendeEnhetConsumer;
    private final BehandleSakConsumer behandleSakConsumer;
    private final OppgaveConsumer oppgaveConsumer;
    private final SykepengesoknadDAO sykepengesoknadDAO;

    @Inject
    public SaksbehandlingService(OppgavebehandlingConsumer oppgavebehandlingConsumer, BehandlendeEnhetConsumer behandlendeEnhetConsumer, BehandleSakConsumer behandleSakConsumer, OppgaveConsumer oppgaveConsumer, SykepengesoknadDAO sykepengesoknadDAO) {
        this.oppgavebehandlingConsumer = oppgavebehandlingConsumer;
        this.behandlendeEnhetConsumer = behandlendeEnhetConsumer;
        this.behandleSakConsumer = behandleSakConsumer;
        this.oppgaveConsumer = oppgaveConsumer;
        this.sykepengesoknadDAO = sykepengesoknadDAO;
    }

    public String behandleInntektsmelding(Inntektsmelding inntektsmelding) {
        Optional<Sykepengesoknad> sykepengesoknad = sykepengesoknadDAO.hentSykepengesoknad(inntektsmelding.getJournalpostId());

        String saksId = sykepengesoknad
                .filter(soknad -> soknad.getStatus().equals("SENDT") || soknad.getStatus().equals("TIL_SENDING"))
                .map(Sykepengesoknad::getSaksId)
                .orElse(behandleSakConsumer.opprettSak(inntektsmelding.getFnr()));

        if (sykepengesoknad.isPresent() && sykepengesoknad.get().getStatus().equals("SENDT") || sykepengesoknad.get().getStatus().equals("TIL_SENDING")) {
            Sykepengesoknad soknad = sykepengesoknad.get();

            Optional<Oppgave> oppgave = oppgaveConsumer.finnOppgave(soknad.getOppgaveId());

            if (oppgave.isPresent() && oppgave.get().getStatus().equals("APEN")) {
                log.info("Fant eksisterende åpen oppgave. Oppdaterete beskrivelsen.");
                oppgavebehandlingConsumer.oppdaterOppgavebeskrivelse(oppgave.get(), "Det har kommet en inntektsmelding på sykepenger.");
            } else {
                log.info("Fant ikke eksisterende åpen oppgave. Opprettet ny oppgave.");
                opprettOppgave(inntektsmelding.getFnr(), byggOppgave(inntektsmelding.getJournalpostId(), saksId));
            }
        } else {
            log.info("Fant ikke tilhørende åpen sak for journalpost: {}. Opprettet ny sak og ny oppgave med sak: {}.", inntektsmelding.getJournalpostId(), saksId);
            opprettOppgave(inntektsmelding.getFnr(), byggOppgave(inntektsmelding.getJournalpostId(), saksId));
        }

        return saksId;
    }

    private void opprettOppgave(String fnr, Oppgave oppgave) {
        String behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(fnr);
        String geografiskTilknytning = behandlendeEnhetConsumer.hentGeografiskTilknytning(fnr);

        Oppgave nyOppgave = Oppgave.builder()
                .aktivTil(oppgave.getAktivTil())
                .beskrivelse(oppgave.getBeskrivelse())
                .gsakSaksid(oppgave.getGsakSaksid())
                .journalpostId(oppgave.getJournalpostId())
                .geografiskTilknytning(geografiskTilknytning)
                .behandlendeEnhetId(behandlendeEnhet)
                .build();

        String oppgaveId = oppgavebehandlingConsumer.opprettOppgave(fnr, nyOppgave);
        log.info("Opprettet oppgave: {}", oppgaveId);
    }

    private Oppgave byggOppgave(String journalpostId, String saksId) {
        return Oppgave.builder()
                .journalpostId(journalpostId)
                .gsakSaksid(saksId)
                .beskrivelse("Det har kommet en inntektsmelding på sykepenger.")
                .aktivTil(LocalDate.now().plusDays(7))
                .build();
    }
}
