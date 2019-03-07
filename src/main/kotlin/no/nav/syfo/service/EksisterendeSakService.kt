package no.nav.syfo.service

import no.nav.syfo.consumer.rest.EksisterendeSakConsumer
import no.nav.syfo.consumer.SakConsumer
import org.springframework.stereotype.Service

@Service
class EksisterendeSakService(
        val eksisterendeSakConsumer: EksisterendeSakConsumer,
        val sakConsumer: SakConsumer) {

    fun finnEksisterendeSak(aktorId: String, orgnummer: String?): String? {
        val maybeSakFraSyfoservice = eksisterendeSakConsumer.finnEksisterendeSaksId(aktorId, orgnummer).orElse(null)
        val maybeSakFraSyfogsak = sakConsumer.finnSisteSak(aktorId)

        return listOfNotNull(maybeSakFraSyfogsak, maybeSakFraSyfoservice)
                .sortedDescending()
                .firstOrNull()
    }
}
