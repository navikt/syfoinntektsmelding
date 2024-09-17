package no.nav.syfo

import io.prometheus.client.Counter
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsvarsler

const val METRICS_NS = "spinn"

class MetrikkVarsler : Bakgrunnsvarsler {
    override fun rapporterPermanentFeiletJobb() {
        FEILET_JOBB_COUNTER.inc()
    }
}

val FEILET_JOBB_COUNTER = Counter.build()
    .namespace(METRICS_NS)
    .name("feilet_jobb")
    .help("Counts the number of permanently failed jobs")
    .register()
