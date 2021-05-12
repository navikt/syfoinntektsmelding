package no.nav.syfo.web.nais

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.ProbeResult
import no.nav.helse.arbeidsgiver.kubernetes.ProbeState
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import java.util.*

fun Application.nais() {

    val logger = LoggerFactory.getLogger("Helsesjekker")

    suspend fun PipelineContext<Unit, ApplicationCall>.returnResultOfChecks(checkResults: ProbeResult) {
        val httpResult =
            if (checkResults.state == ProbeState.UN_HEALTHY) HttpStatusCode.InternalServerError else HttpStatusCode.OK
        checkResults.unhealthyComponents.forEach { r ->
            r.error?.let { logger.error("Helsejekk feiler for ${r.componentName}", it) }
        }
        call.respond(httpResult, checkResults)
    }

    routing {
        get("/health/is-alive") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val checkResults = kubernetesProbeManager.runLivenessProbe()
            returnResultOfChecks(checkResults)
        }

        get("/health/is-ready") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val checkResults = kubernetesProbeManager.runReadynessProbe()
            returnResultOfChecks(checkResults)
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: Collections.emptySet()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
            }
        }

        get("/healthcheck") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val readyResults = kubernetesProbeManager.runReadynessProbe()
            val liveResults = kubernetesProbeManager.runLivenessProbe()
            val combinedResults = ProbeResult(
                liveResults.healthyComponents +
                    liveResults.unhealthyComponents +
                    readyResults.healthyComponents +
                    readyResults.unhealthyComponents
            )

            returnResultOfChecks(combinedResults)
        }
    }
}

