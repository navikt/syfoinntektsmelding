package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.behandlesak.v2.*
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
class BehandleSakConsumer @Inject
constructor(private val behandleSakV2: BehandleSakV2, private val metrikk: Metrikk) {

    var log = log()

    fun opprettSak(fnr: String): String {
        try {
            val aktoer = WSAktor().withIdent(fnr)
            val sakId = behandleSakV2.opprettSak(
                    WSOpprettSakRequest().withSak(
                            WSSak()
                                    .withSaktype("GEN")
                                    .withFagomrade("SYK")
                                    .withFagsystem("FS22")
                                    .withGjelderBrukerListe(aktoer)
                    )
            ).sakId
            log.info("Opprettet ny sak")
            metrikk.tellInntektsmeldingNySak()
            return sakId
        } catch (e: WSSakEksistererAlleredeException) {
            log.error("Sak finnes allerede", e)
            throw RuntimeException("Sak finnes allerede", e)
        } catch (e: WSUgyldigInputException) {
            log.error("Ugyldig input", e)
            throw RuntimeException("Ugyldid input i sak", e)
        }
    }
}
