package no.nav.syfo.service

import log
import no.nav.syfo.consumer.rest.EksisterendeSakConsumer
import no.nav.syfo.consumer.SakConsumer
import org.springframework.stereotype.Service

@Service
class EksisterendeSakService(
        val eksisterendeSakConsumer: EksisterendeSakConsumer,
        val sakConsumer: SakConsumer) {

    val log = log()

    fun finnEksisterendeSak(aktorId: String, orgnummer: String?): String? {
        val maybeSakFraSyfoservice = eksisterendeSakConsumer.finnEksisterendeSaksId(aktorId, orgnummer).orElse(null)
        val maybeSakFraSyfogsak = sakConsumer.finnSisteSak(aktorId)

        log.info("Sak fra service: {}, sak fra syfogsak: {}", maybeSakFraSyfoservice, maybeSakFraSyfogsak)

        return listOfNotNull(maybeSakFraSyfogsak, maybeSakFraSyfoservice)
                .sortedDescending()
                .firstOrNull()
    }
}
