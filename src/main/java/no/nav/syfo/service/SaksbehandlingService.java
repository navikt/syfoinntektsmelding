package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.rest.AktorConsumer;
import no.nav.syfo.consumer.ws.*;
import no.nav.syfo.domain.*;
import no.nav.syfo.repository.SykepengesoknadDAO;
import no.nav.syfo.repository.SykmeldingDAO;
import no.nav.tjeneste.virksomhet.aktoer.v2.HentAktoerIdForIdentPersonIkkeFunnet;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
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
    private final SykepengesoknadDAO sykepengesoknadDAO;
    private final AktorConsumer aktorConsumer;
    private final SykmeldingDAO sykmeldingDAO;

    @Inject
    public SaksbehandlingService(
            OppgavebehandlingConsumer oppgavebehandlingConsumer,
            BehandlendeEnhetConsumer behandlendeEnhetConsumer,
            BehandleSakConsumer behandleSakConsumer,
            SykepengesoknadDAO sykepengesoknadDAO,
            AktorConsumer aktorConsumer, SykmeldingDAO sykmeldingDAO) {
        this.oppgavebehandlingConsumer = oppgavebehandlingConsumer;
        this.behandlendeEnhetConsumer = behandlendeEnhetConsumer;
        this.behandleSakConsumer = behandleSakConsumer;
        this.sykepengesoknadDAO = sykepengesoknadDAO;
        this.aktorConsumer = aktorConsumer;
        this.sykmeldingDAO = sykmeldingDAO;
    }

    public String behandleInntektsmelding(Inntektsmelding inntektsmelding) {
        String aktorid = aktorConsumer.getAktorId(inntektsmelding.getFnr());

        Optional<Sykepengesoknad> sisteSykepengesoknad = hentSykepengesoknader(aktorid, inntektsmelding.getArbeidsgiverOrgnummer())
                .stream()
                .max(comparing(Sykepengesoknad::getTom));

        String saksId = sisteSykepengesoknad.isPresent()
                ? sisteSykepengesoknad.get().getSaksId()
                : behandleSakConsumer.opprettSak(inntektsmelding.getFnr());

        opprettOppgave(inntektsmelding.getFnr(), byggOppgave(inntektsmelding.getJournalpostId(), saksId));

        return saksId;
    }

    private List<Sykepengesoknad> hentSykepengesoknader(String aktorid, String orgnummer) {
        List<Integer> sykmeldinger = hentSykmeldingIder(aktorid, orgnummer);
        return sykmeldinger
                .stream()
                .map(sykmeldingid -> sykepengesoknadDAO.hentSykepengesoknaderForPerson(aktorid, sykmeldingid))
                .flatMap(List::stream)
                .collect(toList());
    }

    private List<Integer> hentSykmeldingIder(String aktorid, String orgnummer) {
        return sykmeldingDAO.hentSykmeldingerForOrgnummer(orgnummer, aktorid)
                .stream()
                .map(Sykmelding::getId)
                .collect(toList());
    }

    private void opprettOppgave(String fnr, Oppgave oppgave) {
        String behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(fnr);
        GeografiskTilknytningData geografiskTilknytning = behandlendeEnhetConsumer.hentGeografiskTilknytning(fnr);

        Oppgave nyOppgave = Oppgave.builder()
                .aktivTil(oppgave.getAktivTil())
                .beskrivelse(oppgave.getBeskrivelse())
                .saksnummer(oppgave.getSaksnummer())
                .dokumentId(oppgave.getDokumentId())
                .geografiskTilknytning(geografiskTilknytning.geografiskTilknytning)
                .ansvarligEnhetId(behandlendeEnhet)
                .build();

        oppgavebehandlingConsumer.opprettOppgave(fnr, nyOppgave);
    }

    private Oppgave byggOppgave(String dokumentId, String saksnummer) {
        return Oppgave.builder()
                .dokumentId(dokumentId)
                .saksnummer(saksnummer)
                .beskrivelse("Det har kommet en inntektsmelding på sykepenger.")
                .aktivTil(LocalDate.now().plusDays(7))
                .build();
    }
}
