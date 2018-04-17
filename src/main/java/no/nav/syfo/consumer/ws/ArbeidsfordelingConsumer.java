package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.ArbeidsfordelingV1;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.FinnBehandlendeEnhetListeUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSArbeidsfordelingKriterier;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSGeografi;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSOrganisasjonsenhet;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSTema;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.WSFinnBehandlendeEnhetListeRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSEnhetsstatus.AKTIV;

@Component
@Slf4j
public class ArbeidsfordelingConsumer {

    private final ArbeidsfordelingV1 arbeidsfordelingV1;

    @Inject
    public ArbeidsfordelingConsumer(ArbeidsfordelingV1 arbeidsfordelingV1) {
        this.arbeidsfordelingV1 = arbeidsfordelingV1;
    }

    public String finnBehandlendeEnhet(String geografiskTilknytning) {
        try {
            return arbeidsfordelingV1.finnBehandlendeEnhetListe(new WSFinnBehandlendeEnhetListeRequest()
                    .withArbeidsfordelingKriterier(new WSArbeidsfordelingKriterier()
                            .withGeografiskTilknytning(new WSGeografi().withValue(geografiskTilknytning))
                            .withTema(new WSTema().withValue("SYK"))))
                    .getBehandlendeEnhetListe()
                    .stream()
                    .filter(wsOrganisasjonsenhet -> AKTIV.equals(wsOrganisasjonsenhet.getStatus()))
                    .map(WSOrganisasjonsenhet::getEnhetId)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Fant ingen aktiv enhet for " + geografiskTilknytning));
        } catch (FinnBehandlendeEnhetListeUgyldigInput e) {
            log.error("Feil ved henting av brukers forvaltningsenhet", e);
            throw new RuntimeException("Feil ved henting av brukers forvaltningsenhet", e);
        }
    }
}
