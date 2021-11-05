package no.nav.syfo.service

import java.time.LocalDate
import log
import no.nav.syfo.client.SakConsumer

class EksisterendeSakService(
    private val sakConsumer: SakConsumer
) {

    val log = log()

    fun finnEksisterendeSak(aktorId: String, fom: LocalDate?, tom: LocalDate?): String? {
        return sakConsumer.finnSisteSak(aktorId, fom, tom)
            ?.also { log.info("Sak fra syfogsak: {}", it) }
    }
}
