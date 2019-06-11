package no.nav.syfo.service

import log
import no.nav.syfo.consumer.SakConsumer
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EksisterendeSakService(
    val sakConsumer: SakConsumer
) {

    val log = log()

    fun finnEksisterendeSak(aktorId: String, fom:LocalDate?, tom:LocalDate?): String? {
        val maybeSakFraSyfogsak = sakConsumer.finnSisteSak(aktorId, fom, tom)

        log.info("Sak fra service: {}, sak fra syfogsak: {}", maybeSakFraSyfogsak)

        return maybeSakFraSyfogsak
    }
}
