package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.FinnBehandlendeEnhetListeUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSArbeidsfordelingKriterier
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSDiskresjonskoder
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSEnhetsstatus.AKTIV
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSGeografi
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.WSTema
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.WSFinnBehandlendeEnhetListeRequest
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import org.springframework.stereotype.Component

@Component
class BehandlendeEnhetConsumer(
    private val personV3: PersonV3,
    private val arbeidsfordelingV1: ArbeidsfordelingV1,
    private val metrikk: Metrikk
) {

    var log = log()

    fun hentBehandlendeEnhet(fnr: String): String {
        val geografiskTilknytning = hentGeografiskTilknytning(fnr)

        try {
            val behandlendeEnhet = arbeidsfordelingV1.finnBehandlendeEnhetListe(
                WSFinnBehandlendeEnhetListeRequest()
                    .withArbeidsfordelingKriterier(
                        WSArbeidsfordelingKriterier()
                            .withDiskresjonskode(
                                if (geografiskTilknytning.diskresjonskode != null)
                                    WSDiskresjonskoder().withValue(geografiskTilknytning.diskresjonskode)
                                else
                                    null
                            )
                            .withGeografiskTilknytning(WSGeografi().withValue(geografiskTilknytning.geografiskTilknytning))
                            .withTema(WSTema().withValue("SYK"))
                    )
            )
                .behandlendeEnhetListe
                .stream()
                .filter { wsOrganisasjonsenhet -> AKTIV == wsOrganisasjonsenhet.status }
                .map { it.enhetId }
                .findFirst()
                .orElseThrow { RuntimeException("Fant ingen aktiv enhet for " + geografiskTilknytning.geografiskTilknytning) }

            // 4474 er enhetsnummeret til sykepenger utland
            if ("4474" == behandlendeEnhet) {
                log.info(
                    "Behandlende enhet er 4474. Med geografisk tilknytning: {}",
                    geografiskTilknytning.geografiskTilknytning
                )
                metrikk.tellInntektsmeldingSykepengerUtland()
            }

            return behandlendeEnhet

        } catch (e: FinnBehandlendeEnhetListeUgyldigInput) {
            log.error("Feil ved henting av brukers forvaltningsenhet", e)
            throw RuntimeException("Feil ved henting av brukers forvaltningsenhet", e)
        } catch (e: RuntimeException) {
            log.error("Klarte ikke å hente behandlende enhet!", e)
            throw RuntimeException(e)
        }

    }

    fun hentGeografiskTilknytning(fnr: String): GeografiskTilknytningData {
        try {
            val response = personV3.hentGeografiskTilknytning(
                HentGeografiskTilknytningRequest()
                    .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(fnr)))
            )

            return GeografiskTilknytningData(
                geografiskTilknytning = response.geografiskTilknytning?.geografiskTilknytning,
                diskresjonskode = response.diskresjonskode?.value
            )
        } catch (e: HentGeografiskTilknytningSikkerhetsbegrensing) {
            log.error("Feil ved henting av geografisk tilknytning", e)
            throw RuntimeException("Feil ved henting av geografisk tilknytning", e)
        } catch (e: HentGeografiskTilknytningPersonIkkeFunnet) {
            log.error("Feil ved henting av geografisk tilknytning", e)
            throw RuntimeException("Feil ved henting av geografisk tilknytning", e)
        }

    }

}
