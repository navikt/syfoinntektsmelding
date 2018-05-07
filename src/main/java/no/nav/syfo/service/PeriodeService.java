package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.ws.BehandleSakConsumer;
import no.nav.syfo.consumer.ws.OppgaveConsumer;
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.domain.Sykepengesoknad;
import no.nav.syfo.repository.SykepengesoknadDAO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@Slf4j
public class PeriodeService {

    private SykepengesoknadDAO sykepengesoknadDAO;
    private OppgaveConsumer oppgaveConsumer;
    private BehandleSakConsumer behandleSakConsumer;
    private SaksbehandlingService saksbehandlingService;
    private OppgavebehandlingConsumer oppgavebehandlingConsumer;


    public PeriodeService(SykepengesoknadDAO sykepengesoknadDAO, OppgaveConsumer oppgaveConsumer, BehandleSakConsumer behandleSakConsumer, SaksbehandlingService saksbehandlingService, OppgavebehandlingConsumer oppgavebehandlingConsumer) {
        this.sykepengesoknadDAO = sykepengesoknadDAO;
        this.oppgaveConsumer = oppgaveConsumer;
        this.behandleSakConsumer = behandleSakConsumer;
        this.saksbehandlingService = saksbehandlingService;
        this.oppgavebehandlingConsumer = oppgavebehandlingConsumer;
    }

    public boolean erSendtInnSoknadForPeriode(String journalpostId) {
        boolean harApenSoknadIPeriode = sykepengesoknadDAO.finnSykepengesoknad(journalpostId)
                .map(Sykepengesoknad::getStatus)
                .filter("APEN"::equals)
                .isPresent();

        log.info("Er sendt inn søknad for periode: {}", harApenSoknadIPeriode);

        return harApenSoknadIPeriode;
    }

    public boolean oppgaveErOpprettetForSak(String oppgaveId) {
        boolean harApenOppgaveForSak = oppgaveConsumer.finnOppgave(oppgaveId)
                .map(Oppgave::getStatus)
                .filter("APEN"::equals)
                .isPresent();

        log.info("Finnes åpen oppgave for sak: {}", harApenOppgaveForSak);

        return harApenOppgaveForSak;
    }

    public String samleSaksinformasjon(Inntektsmelding inntektsmelding) {
        String saksId;
        if (erSendtInnSoknadForPeriode(inntektsmelding.getJournalpostId())) {
            Optional<Sykepengesoknad> sykepengesoknad = sykepengesoknadDAO.finnSykepengesoknad(inntektsmelding.getJournalpostId());
            saksId = sykepengesoknad.get().getSaksId();
            log.info("Fant sak: {}, for journalpost: {}.", saksId, inntektsmelding.getJournalpostId());

            if (oppgaveErOpprettetForSak(sykepengesoknad.get().getOppgaveId())) {
                Oppgave oppgave = oppgaveConsumer.finnOppgave(sykepengesoknad.get().getOppgaveId()).get();
                oppgavebehandlingConsumer.oppdaterOppgavebeskrivelse(oppgave, "Det har kommet en inntektsmelding på sykepenger.");
                log.info("Fant eksisterende åpen oppgave. Oppdaterete beskrivelsen.");
            } else {
                saksbehandlingService.opprettOppgave(inntektsmelding.getFnr(), byggOppgave(inntektsmelding.getJournalpostId(), saksId));
                log.info("Fant ikke eksisterende åpen oppgave. Opprettet ny oppgave.");
            }
        } else {
            saksId = behandleSakConsumer.opprettSak(inntektsmelding.getFnr());
            saksbehandlingService.opprettOppgave(inntektsmelding.getFnr(), byggOppgave(inntektsmelding.getJournalpostId(), saksId));
            log.info("Fant ikke tilhørende åpen sak for journalpost: {}. Opprettet ny sak og ny oppgave med sak: {}.", inntektsmelding.getJournalpostId(), saksId);
        }
        return saksId;
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
