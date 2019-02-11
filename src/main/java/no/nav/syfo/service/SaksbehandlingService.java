package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.rest.EksisterendeSakConsumer;
import no.nav.syfo.consumer.ws.BehandleSakConsumer;
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer;
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer;
import no.nav.syfo.domain.GeografiskTilknytningData;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.domain.Periode;
import no.nav.syfo.repository.InntektsmeldingDAO;
import no.nav.syfo.repository.InntektsmeldingMeta;
import no.nav.syfo.util.DateUtil;
import no.nav.syfo.util.Metrikk;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class SaksbehandlingService {
    private final OppgavebehandlingConsumer oppgavebehandlingConsumer;
    private final BehandlendeEnhetConsumer behandlendeEnhetConsumer;
    private final BehandleSakConsumer behandleSakConsumer;
    private final EksisterendeSakConsumer eksisterendeSakConsumer;
    private final InntektsmeldingDAO inntektsmeldingDAO;
    private final Metrikk metrikk;

    @Inject
    public SaksbehandlingService(
            OppgavebehandlingConsumer oppgavebehandlingConsumer,
            BehandlendeEnhetConsumer behandlendeEnhetConsumer,
            BehandleSakConsumer behandleSakConsumer,
            EksisterendeSakConsumer eksisterendeSakConsumer,
            InntektsmeldingDAO inntektsmeldingDAO, Metrikk metrikk) {
        this.oppgavebehandlingConsumer = oppgavebehandlingConsumer;
        this.behandlendeEnhetConsumer = behandlendeEnhetConsumer;
        this.behandleSakConsumer = behandleSakConsumer;
        this.eksisterendeSakConsumer = eksisterendeSakConsumer;
        this.inntektsmeldingDAO = inntektsmeldingDAO;
        this.metrikk = metrikk;
    }

    private boolean helper(List<Periode> perioder, Periode periode) {
        return perioder.stream()
                .anyMatch(p -> DateUtil.overlapperPerioder(p, periode));
    }

    private Optional<InntektsmeldingMeta> finnTilhorendeInntektsmelding(Inntektsmelding inntektsmelding, String aktorId) {
        return inntektsmeldingDAO.finnBehandledeInntektsmeldinger(aktorId)
                .stream()
                .filter(im -> im.getArbeidsgiverperioder()
                        .stream()
                        .anyMatch(p -> helper(inntektsmelding.getArbeidsgiverperioder(), p)))
                .findFirst();
    }


    public String behandleInntektsmelding(Inntektsmelding inntektsmelding, String aktorId) {

        Optional<InntektsmeldingMeta> tilhorendeInntektsmelding = finnTilhorendeInntektsmelding(inntektsmelding, aktorId);
        tilhorendeInntektsmelding.ifPresent(inntektsmeldingMeta -> {
            log.info("Fant overlappende inntektsmelding, bruker samme saksId: {}", inntektsmeldingMeta.getSakId());
            metrikk.tellOverlappendeInntektsmelding();
        });

        String saksId = tilhorendeInntektsmelding
                .map(InntektsmeldingMeta::getSakId)
                .orElseGet(() -> inntektsmelding.getArbeidsgiverOrgnummer()
                        .flatMap(orgnummer -> eksisterendeSakConsumer.finnEksisterendeSaksId(aktorId, orgnummer))
                        .orElseGet(() -> behandleSakConsumer.opprettSak(inntektsmelding.getFnr())));

        opprettOppgave(inntektsmelding.getFnr(), byggOppgave(inntektsmelding.getJournalpostId(), saksId));

        return saksId;
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
