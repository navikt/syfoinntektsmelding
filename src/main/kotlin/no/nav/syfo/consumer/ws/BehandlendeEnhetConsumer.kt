package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.behandling.BehandlendeEnhetFeiletException
import no.nav.syfo.behandling.FinnBehandlendeEnhetListeUgyldigInputException
import no.nav.syfo.behandling.HentGeografiskTilknytningPersonIkkeFunnetException
import no.nav.syfo.behandling.HentGeografiskTilknytningSikkerhetsbegrensingException
import no.nav.syfo.behandling.IngenAktivEnhetException
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnBehandlendeEnhetListeUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.*
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeRequest
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import org.springframework.stereotype.Component

const val SYKEPENGER_UTLAND = "4474"

@Component
class BehandlendeEnhetConsumer(
    private val personV3: PersonV3,
    private val arbeidsfordelingV1: ArbeidsfordelingV1,
    private val metrikk: Metrikk
) {

    var log = log()

    fun hentBehandlendeEnhet(fnr: String, uuid: String): String {
        val geografiskTilknytning = hentGeografiskTilknytning(fnr)

        val tema = Tema()
        tema.value = "SYK"

        val disk = Diskresjonskoder()
        disk.value = geografiskTilknytning.diskresjonskode

        val geo = Geografi()
        geo.value = geografiskTilknytning.geografiskTilknytning

        val kriterier = ArbeidsfordelingKriterier()
        kriterier.tema = tema
        kriterier.diskresjonskode = null
        kriterier.geografiskTilknytning = geo


        if (geografiskTilknytning.diskresjonskode != null){
            kriterier.diskresjonskode = disk
        }

        val request = FinnBehandlendeEnhetListeRequest()
        request.arbeidsfordelingKriterier = kriterier

        try {
            val behandlendeEnhet = arbeidsfordelingV1.finnBehandlendeEnhetListe(request)
                    .behandlendeEnhetListe
                    .stream()
                    .filter { wsOrganisasjonsenhet -> Enhetsstatus.AKTIV == wsOrganisasjonsenhet.status }
                    .map { it.enhetId }
                    .findFirst()
                    .orElseThrow { IngenAktivEnhetException(geografiskTilknytning.geografiskTilknytning, null) }

            if (SYKEPENGER_UTLAND == behandlendeEnhet) {
                metrikk.tellInntektsmeldingSykepengerUtland()
            }
            log.info("Fant geografiskTilknytning ${geografiskTilknytning.geografiskTilknytning} med behandlendeEnhet $behandlendeEnhet for inntektsmelding $uuid");
            return behandlendeEnhet

        } catch (e: FinnBehandlendeEnhetListeUgyldigInput) {
            log.error("Feil ved henting av brukers forvaltningsenhet", e)
            throw FinnBehandlendeEnhetListeUgyldigInputException(e)
        } catch (e: RuntimeException) {
            log.error("Klarte ikke å hente behandlende enhet!", e)
            throw BehandlendeEnhetFeiletException(e)
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
            throw HentGeografiskTilknytningSikkerhetsbegrensingException(e)
        } catch (e: HentGeografiskTilknytningPersonIkkeFunnet) {
            log.error("Feil ved henting av geografisk tilknytning", e)
            throw HentGeografiskTilknytningPersonIkkeFunnetException(e)
        }

    }

}
