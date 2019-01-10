package no.nav.syfo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.consumer.rest.EksisterendeSakConsumer;
import no.nav.syfo.consumer.ws.BehandleSakConsumer;
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer;
import no.nav.syfo.consumer.ws.OppgavebehandlingConsumer;
import no.nav.syfo.domain.GeografiskTilknytningData;
import no.nav.syfo.domain.Inntektsmelding;
import no.nav.syfo.domain.Oppgave;
import no.nav.syfo.repository.InntektsmeldingDAO;
import no.nav.syfo.repository.InntektsmeldingMeta;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

import static no.nav.syfo.util.DateUtil.compare;

@Component
@Slf4j
public class SaksbehandlingService {
    private final OppgavebehandlingConsumer oppgavebehandlingConsumer;
    private final BehandlendeEnhetConsumer behandlendeEnhetConsumer;
    private final BehandleSakConsumer behandleSakConsumer;
    private final EksisterendeSakConsumer eksisterendeSakConsumer;
    private final InntektsmeldingDAO inntektsmeldingDAO;

    @Inject
    public SaksbehandlingService(
            OppgavebehandlingConsumer oppgavebehandlingConsumer,
            BehandlendeEnhetConsumer behandlendeEnhetConsumer,
            BehandleSakConsumer behandleSakConsumer,
            EksisterendeSakConsumer eksisterendeSakConsumer,
            InntektsmeldingDAO inntektsmeldingDAO) {
        this.oppgavebehandlingConsumer = oppgavebehandlingConsumer;
        this.behandlendeEnhetConsumer = behandlendeEnhetConsumer;
        this.behandleSakConsumer = behandleSakConsumer;
        this.eksisterendeSakConsumer = eksisterendeSakConsumer;
        this.inntektsmeldingDAO = inntektsmeldingDAO;
    }

    private Optional<InntektsmeldingMeta> finnTilhorendeInntektsmelding(Inntektsmelding inntektsmelding, String aktorId) {
        return inntektsmeldingDAO.finnBehandledeInntektsmeldinger(aktorId, inntektsmelding.getArbeidsgiverOrgnummer())
                .stream()
                .filter(tidligereBehandletInntektsmelding -> {
                    LocalDate fom = tidligereBehandletInntektsmelding.getArbeidsgiverperiodeFom();
                    LocalDate tom = tidligereBehandletInntektsmelding.getArbeidsgiverperiodeTom();

                    return compare(inntektsmelding.getArbeidsgiverperiodeTom()).isBetweenOrEqual(fom, tom) ||
                            compare(inntektsmelding.getArbeidsgiverperiodeFom()).isBetweenOrEqual(fom, tom);
                })
                .findFirst();
    }


    public String behandleInntektsmelding(Inntektsmelding inntektsmelding, String aktorId) {

        Optional<InntektsmeldingMeta> tilhorendeInntektsmelding = finnTilhorendeInntektsmelding(inntektsmelding, aktorId);

        String saksId = tilhorendeInntektsmelding
                .map(InntektsmeldingMeta::getSakId)
                .orElseGet(() -> eksisterendeSakConsumer.finnEksisterendeSaksId(aktorId, inntektsmelding.getArbeidsgiverOrgnummer())
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
