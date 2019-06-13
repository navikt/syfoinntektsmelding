package no.nav.syfo.consumer.ws

import log
import no.nav.syfo.util.Metrikk
import no.nav.tjeneste.virksomhet.behandlesak.v1.BehandleSakV1
import no.nav.tjeneste.virksomhet.behandlesak.v1.OpprettSakSakEksistererAllerede
import no.nav.tjeneste.virksomhet.behandlesak.v1.OpprettSakUgyldigInput
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.*
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.WSOpprettSakRequest
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
class BehandleSakConsumer @Inject
constructor(private val behandleSakV1: BehandleSakV1, private val metrikk: Metrikk) {

    var log = log()

    fun opprettSak(fnr: String): String {
        try {
            val sakId = behandleSakV1.opprettSak(
                WSOpprettSakRequest()
                    .withSak(
                        WSSak()
                            .withSakstype(WSSakstyper().withValue("GEN"))
                            .withFagomraade(WSFagomraader().withValue("SYK"))
                            .withFagsystem(WSFagsystemer().withValue("FS22"))
                            .withGjelderBrukerListe(WSPerson().withIdent(fnr))
                    )
            ).sakId

            log.info("Opprettet ny sak")
            metrikk.tellInntektsmeldingNySak()
            return sakId
        } catch (e: OpprettSakSakEksistererAllerede) {
            log.error("Sak finnes allerede", e)
            throw RuntimeException("Sak finnes allerede", e)
        } catch (e: OpprettSakUgyldigInput) {
            log.error("Ugyldig input", e)
            throw RuntimeException("Ugyldid input i sak", e)
        }

    }
}
