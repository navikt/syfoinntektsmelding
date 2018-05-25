package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.domain.Sykmelding;
import no.nav.syfo.repository.SykepengesoknadDAO;
import no.nav.syfo.repository.SykmeldingDAO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class SaksbehandlingService {
    private final OppgavebehandlingConsumer oppgavebehandlingConsumer;
    private final BehandlendeEnhetConsumer behandlendeEnhetConsumer;
    private final BehandleSakConsumer behandleSakConsumer;
    private final OppgaveConsumer oppgaveConsumer;
    private final SykepengesoknadDAO sykepengesoknadDAO;
    private final AktoridConsumer aktoridConsumer;
    private final SykmeldingDAO sykmeldingDAO;

    @Inject
    public SaksbehandlingService(OppgavebehandlingConsumer oppgavebehandlingConsumer, BehandlendeEnhetConsumer behandlendeEnhetConsumer, BehandleSakConsumer behandleSakConsumer, OppgaveConsumer oppgaveConsumer, SykepengesoknadDAO sykepengesoknadDAO, AktoridConsumer aktoridConsumer, SykmeldingDAO sykmeldingDAO) {
        this.oppgavebehandlingConsumer = oppgavebehandlingConsumer;
        this.behandlendeEnhetConsumer = behandlendeEnhetConsumer;
        this.behandleSakConsumer = behandleSakConsumer;
        this.oppgaveConsumer = oppgaveConsumer;
        this.sykepengesoknadDAO = sykepengesoknadDAO;
        this.aktoridConsumer = aktoridConsumer;
        this.sykmeldingDAO = sykmeldingDAO;
    }

    public String behandleInntektsmelding(Inntektsmelding inntektsmelding) {
        String aktorid = aktoridConsumer.hentAktoerIdForFnr(inntektsmelding.getFnr());

        List<Sykepengesoknad> sykepengesoknader = hentSykepengesoknader(aktorid, inntektsmelding.getArbeidsgiverOrgnummer());

        Optional<Sykepengesoknad> sisteSykepengesoknad = finnSisteSoknad(sykepengesoknader);

        String saksId = sisteSykepengesoknad.isPresent()
                ? sisteSykepengesoknad.get().getSaksId()
                : behandleSakConsumer.opprettSak(inntektsmelding.getFnr());

        if (sisteSykepengesoknad.isPresent()) {
            Optional<Oppgave> oppgave = oppgaveConsumer.finnOppgave(sisteSykepengesoknad.get().getOppgaveId());

            if (oppgave.isPresent() && oppgave.get().getStatus().equals("A")) {
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

    private Optional<Sykepengesoknad> finnSisteSoknad(List<Sykepengesoknad> sykepengesoknader) {
        return sykepengesoknader.isEmpty()
                ? Optional.empty()
                : sykepengesoknader.stream()
                .max(comparing(Sykepengesoknad::getTom));
    }

    private List<Integer> hentSykmeldingIder(String aktorid, String orgnummer) {
        return sykmeldingDAO.hentSykmeldingerForOrgnummer(orgnummer, aktorid).stream()
                .map(Sykmelding::getId)
                .collect(toList());
    }

    private List<Sykepengesoknad> hentSykepengesoknader(String aktorid, String orgnummer) {
        List<Integer> sykmeldinger = hentSykmeldingIder(aktorid, orgnummer);
        return sykmeldinger.isEmpty() ?
                Collections.emptyList()
                : sykmeldinger.stream()
                .map(sykmeldingid -> sykepengesoknadDAO.hentSykepengesoknaderForPerson(aktorid, sykmeldingid))
                .flatMap(List::stream)
                .collect(toList());
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
