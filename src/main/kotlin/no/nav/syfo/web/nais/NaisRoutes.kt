package no.nav.syfo.web.nais

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.pipeline.PipelineContext
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.helse.arbeidsgiver.kubernetes.KubernetesProbeManager
import no.nav.helse.arbeidsgiver.kubernetes.ProbeResult
import no.nav.helse.arbeidsgiver.kubernetes.ProbeState
import no.nav.syfo.util.logger
import org.koin.ktor.ext.get
import java.util.Collections

fun Application.nais() {

    val logger = logger("Helsesjekker")

    suspend fun PipelineContext<Unit, ApplicationCall>.returnResultOfChecks(checkResults: ProbeResult) {
        val httpResult =
            if (checkResults.state == ProbeState.UN_HEALTHY) HttpStatusCode.InternalServerError else HttpStatusCode.OK
        checkResults.unhealthyComponents.forEach { r ->
            r.error?.let { logger.error("Helsesjekk feiler for ${r.componentName}", it) }
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
