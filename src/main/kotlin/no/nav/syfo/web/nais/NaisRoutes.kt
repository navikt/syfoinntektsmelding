package no.nav.syfo.web.nais

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.util.KubernetesProbeManager
import no.nav.syfo.util.ProbeResult
import no.nav.syfo.util.ProbeState
import org.koin.ktor.ext.get
import java.util.Collections

fun Application.nais() {
    val logger = logger()
    val sikkerlogger = sikkerLogger()

    suspend fun RoutingContext.returnResultOfChecks(checkResults: ProbeResult) {
        val httpResult =
            if (checkResults.state == ProbeState.UN_HEALTHY) HttpStatusCode.InternalServerError else HttpStatusCode.OK
        checkResults.unhealthyComponents.forEach { r ->
            r.error?.also {
                logger.error("Helsesjekk feiler for ${r.componentName}")
                sikkerlogger.error("Helsesjekk feiler for ${r.componentName}", it)
            }
        }
        call.respond(httpResult, checkResults)
    }

    routing {
        get("/health/is-alive") {
            val kubernetesProbeManager = KubernetesProbeManager()
            val checkResults = kubernetesProbeManager.runLivenessProbe()
            returnResultOfChecks(checkResults)
        }

        get("/health/is-ready") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val checkResults = kubernetesProbeManager.runReadynessProbe()
            returnResultOfChecks(checkResults)
        }

        get("/metrics") {
            val names =
                call.request.queryParameters
                    .getAll("name[]")
                    ?.toSet() ?: Collections.emptySet()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
            }
        }

        get("/healthcheck") {
            val kubernetesProbeManager = this@routing.get<KubernetesProbeManager>()
            val readyResults = kubernetesProbeManager.runReadynessProbe()
            val liveResults = kubernetesProbeManager.runLivenessProbe()
            val combinedResults =
                ProbeResult(
                    liveResults.healthyComponents +
                        liveResults.unhealthyComponents +
                        readyResults.healthyComponents +
                        readyResults.unhealthyComponents,
                )

            returnResultOfChecks(combinedResults)
        }
    }
}
