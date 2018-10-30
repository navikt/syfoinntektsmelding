package no.nav.syfo.consumer.ws;

import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.ArbeidsfordelingV1;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.FinnBehandlendeEnhetListeUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.WSFinnBehandlendeEnhetListeRequest;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.GeografiskTilknytning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kodeverdi;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.util.Optional.ofNullable;
import static no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSEnhetsstatus.AKTIV;

@Component
@Slf4j
public class BehandlendeEnhetConsumer {

    private final PersonV3 personV3;
    private final ArbeidsfordelingV1 arbeidsfordelingV1;

    @Inject
    public BehandlendeEnhetConsumer(PersonV3 personV3, ArbeidsfordelingV1 arbeidsfordelingV1) {
        this.personV3 = personV3;
        this.arbeidsfordelingV1 = arbeidsfordelingV1;
    }

    public String hentBehandlendeEnhet(String fnr) {
        GeografiskTilknytningDTO geografiskTilknytning = hentGeografiskTilknytning(fnr);

        try {
            return arbeidsfordelingV1.finnBehandlendeEnhetListe(new WSFinnBehandlendeEnhetListeRequest()
                    .withArbeidsfordelingKriterier(new WSArbeidsfordelingKriterier()
                            .withDiskresjonskode(
                                    geografiskTilknytning.diskresjonskode != null
                                            ? new WSDiskresjonskoder().withValue(geografiskTilknytning.diskresjonskode)
                                            : null)
                            .withGeografiskTilknytning(new WSGeografi().withValue(geografiskTilknytning.geografiskTilknytning))
                            .withTema(new WSTema().withValue("SYK"))))
                    .getBehandlendeEnhetListe()
                    .stream()
                    .filter(wsOrganisasjonsenhet -> AKTIV.equals(wsOrganisasjonsenhet.getStatus()))
                    .map(WSOrganisasjonsenhet::getEnhetId)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Fant ingen aktiv enhet for " + geografiskTilknytning.geografiskTilknytning));
        } catch (FinnBehandlendeEnhetListeUgyldigInput e) {
            log.error("Feil ved henting av brukers forvaltningsenhet", e);
            throw new RuntimeException("Feil ved henting av brukers forvaltningsenhet", e);
        } catch (RuntimeException e) {
            log.error("Klarte ikke å hente behandlende enhet!", e);
            throw new RuntimeException(e);
        }
    }

    public GeografiskTilknytningDTO hentGeografiskTilknytning(String fnr) {
        try {
            HentGeografiskTilknytningResponse response = personV3.hentGeografiskTilknytning(new HentGeografiskTilknytningRequest()
                    .withAktoer(new PersonIdent().withIdent(new NorskIdent().withIdent(fnr))));

            return GeografiskTilknytningDTO
                    .builder()
                    .geografiskTilknytning(
                            ofNullable(response.getGeografiskTilknytning())
                                    .map(GeografiskTilknytning::getGeografiskTilknytning)
                                    .orElse(null))
                    .diskresjonskode(
                            ofNullable(response.getDiskresjonskode())
                                    .map(Kodeverdi::getValue)
                                    .orElse(null))
                    .build();
        } catch (HentGeografiskTilknytningSikkerhetsbegrensing | HentGeografiskTilknytningPersonIkkeFunnet e) {
            log.error("Feil ved henting av geografisk tilknytning", e);
            throw new RuntimeException("Feil ved henting av geografisk tilknytning", e);
        }
    }

}
