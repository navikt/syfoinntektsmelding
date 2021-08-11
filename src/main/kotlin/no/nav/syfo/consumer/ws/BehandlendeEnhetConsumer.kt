package no.nav.syfo.consumer.ws

import log
import no.nav.helse.arbeidsgiver.integrasjoner.pdl.PdlClient
import no.nav.syfo.behandling.BehandlendeEnhetFeiletException
import no.nav.syfo.behandling.FinnBehandlendeEnhetListeUgyldigInputException
import no.nav.syfo.behandling.IngenAktivEnhetException
import no.nav.syfo.domain.GeografiskTilknytningData
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.ArbeidsfordelingV1
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnBehandlendeEnhetListeUgyldigInput
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.ArbeidsfordelingKriterier
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Diskresjonskoder
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetsstatus
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Geografi
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Tema
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeRequest

const val SYKEPENGER_UTLAND = "4474"

class BehandlendeEnhetConsumer(
    private val pdlClient: PdlClient,
    private val arbeidsfordelingV1: ArbeidsfordelingV1,
    private val metrikk: Metrikk
)  {

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
            val behandlendeEnhet = finnAktivBehandlendeEnhet(request, geografiskTilknytning)
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

    private fun finnAktivBehandlendeEnhet(
        request: FinnBehandlendeEnhetListeRequest,
        geografiskTilknytning: GeografiskTilknytningData
    ): String {
        val behandlendeEnhet = arbeidsfordelingV1.finnBehandlendeEnhetListe(request)
            .behandlendeEnhetListe
            .stream()
            .filter { wsOrganisasjonsenhet -> Enhetsstatus.AKTIV == wsOrganisasjonsenhet.status }
            .map { it.enhetId }
            .findFirst()
            .orElseThrow { IngenAktivEnhetException(geografiskTilknytning.geografiskTilknytning, null) }
        return behandlendeEnhet
    }


    fun hentGeografiskTilknytning(fnr: String): GeografiskTilknytningData {
        pdlClient.fullPerson(fnr).let {
            return GeografiskTilknytningData(
                geografiskTilknytning = it?.hentGeografiskTilknytning?.hentTilknytning(),
                diskresjonskode = it?.hentPerson?.trekkUtDiskresjonskode()
            )
        }
    }

}
