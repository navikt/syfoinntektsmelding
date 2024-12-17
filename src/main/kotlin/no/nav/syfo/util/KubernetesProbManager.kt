package no.nav.syfo.util

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.system.measureTimeMillis

/**
 * Håndterer komponenter som skal svare på Readyness og Liveness probes fra Kubernetes.
 *
 * https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/
 *
 * Manageren tar registreringer av komponenter i applikasjonen som skal påvirke applikasjonens svar på
 * kubernetes sine Readyness og Liveness-spørringer.
 *
 * Se denne bloggposten for å forstå hva disse er til
 * https://blog.colinbreck.com/kubernetes-liveness-and-readiness-probes-how-to-avoid-shooting-yourself-in-the-foot/
 */
class KubernetesProbeManager {
    private val readynessComponents = HashSet<ReadynessComponent>()
    private val livenessComponents = HashSet<LivenessComponent>()

    fun registerReadynessComponent(component: ReadynessComponent) = readynessComponents.add(component)

    fun registerLivenessComponent(component: LivenessComponent) = livenessComponents.add(component)

    suspend fun runReadynessProbe(): ProbeResult {
        val results =
            readynessComponents.pmap {
                val componentName = it.javaClass.canonicalName
                var runTime = 0L
                try {
                    runTime =
                        measureTimeMillis {
                            it.runReadynessCheck()
                        }
                    ComponentProbeResult(componentName, ProbeState.HEALTHY, runTime)
                } catch (ex: Throwable) {
                    ComponentProbeResult(componentName, ProbeState.UN_HEALTHY, runTime, ex)
                }
            }
        return ProbeResult(results)
    }

    suspend fun runLivenessProbe(): ProbeResult {
        val results =
            livenessComponents.pmap {
                val componentName = it.javaClass.canonicalName
                var runTime = 0L
                try {
                    runTime =
                        measureTimeMillis {
                            it.runLivenessCheck()
                        }
                    ComponentProbeResult(componentName, ProbeState.HEALTHY, runTime)
                } catch (ex: Throwable) {
                    ComponentProbeResult(componentName, ProbeState.UN_HEALTHY, runTime, ex)
                }
            }
        return ProbeResult(results)
    }

    private suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> =
        coroutineScope {
            map { async { f(it) } }.awaitAll()
        }
}

/**
 * Markerer en komponent som en komponent som applikasjonen er avhengig av for å være klar for trafikk.
 */
interface ReadynessComponent {
    /**
     * Implmenterer en selvdiagnose som kaster en exception hvis komponenten ikke er klar for trafikk.
     * Dersom alt er ok returneres ingenting.
     */
    suspend fun runReadynessCheck()
}

/**
 * Representerer en komponent som applikasjonen er avhengig av for å være i live.
 * Hvis en Liveness-komponent er u-frisk blir applikasjonsinstansen drept og en ny instans blir startet.
 */
interface LivenessComponent {
    /**
     * Implmenterer en selvdiagnose som kaster en exception hvis komponenten er død.
     * Dersom alt er ok returneres ingenting.
     */
    suspend fun runLivenessCheck()
}

enum class ProbeState { HEALTHY, UN_HEALTHY }

data class ComponentProbeResult(
    val componentName: String,
    val state: ProbeState,
    val runTime: Long,
    val error: Throwable? = null,
)

/**
 * Hjelpeklasse for å hente ut det samlede resultatet av set sett med sjekker
 */
data class ProbeResult(
    private val resultResults: Collection<ComponentProbeResult>,
) {
    val healthyComponents = resultResults.filter { it.state == ProbeState.HEALTHY }
    val unhealthyComponents = resultResults.filter { it.state == ProbeState.UN_HEALTHY }
    val state = if (unhealthyComponents.isEmpty()) ProbeState.HEALTHY else ProbeState.UN_HEALTHY
}
