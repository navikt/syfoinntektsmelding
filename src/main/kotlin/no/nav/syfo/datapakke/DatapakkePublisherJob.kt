package no.nav.syfo.datapakke

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.syfo.repository.IMStatsRepo
import no.nav.syfo.repository.LPSStats
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

class DatapakkePublisherJob(
    private val imRepo: IMStatsRepo,
    private val httpClient: HttpClient,
    private val datapakkeApiUrl: String,
    private val datapakkeId: String,
    private val applyWeeklyOnly: Boolean = false
) :
    RecurringJob(
        CoroutineScope(Dispatchers.IO),
        Duration.ofHours(1).toMillis()
    ) {

    override fun doJob() {
        val now = LocalDateTime.now()
        if(applyWeeklyOnly && now.dayOfWeek != DayOfWeek.MONDAY && now.hour != 0) {
            return // Ikke kjør jobben med mindre det er natt til mandag
        }

        val datapakkeTemplate = "datapakke/datapakke-spinn.json".loadFromResources()
        val timeseries = imRepo.getWeeklyStats().sortedBy { it.weekNumber }
        val lpsStats = imRepo.getLPSStats()

        val mergedSapStats = lpsStats // SAP rapporterer versjon i feil felt, så de må slås sammen
            .filter { it.lpsNavn.startsWith("SAP") }
            .reduceOrNull { s1, s2 ->
                LPSStats("SAP",
                    s1.antallVersjoner + s2.antallVersjoner,
                    s1.antallInntektsmeldinger + s2.antallInntektsmeldinger
                )
            }

        val filteredLpsStats = lpsStats // Altinn er ikke en LPS, og fjerner alle SAP duplikatene
            .filter { !it.lpsNavn.startsWith("Altinn") }
            .filter { !it.lpsNavn.startsWith("SAP") }
            .toMutableList()

        mergedSapStats?.let { filteredLpsStats.add(it) }

        val arsakStats = imRepo.getArsakStats()

        val populatedDatapakke = datapakkeTemplate
            .replace("@ukeSerie", timeseries.map { it.weekNumber }.joinToString())
            .replace("@total", timeseries.map { it.total }.joinToString())

            .replace("@fraLPS", timeseries.map { it.fraLPS }.joinToString())
            .replace("@fraAltinnPortal", timeseries.map { it.fraAltinnPortal }.joinToString())

            .replace("@fravaer", timeseries.map { it.fravaer }.joinToString())
            .replace("@ikkeFravaer", timeseries.map { it.ikkeFravaer }.joinToString())

            .replace("@arsakEndring", timeseries.map { it.arsakEndring }.joinToString())
            .replace("@arsakNy", timeseries.map { it.arsakNy }.joinToString())

            .replace("@delvisRefusjon", timeseries.map { it.delvisRefusjon }.joinToString())
            .replace("@fullRefusjon", timeseries.map { it.fullRefusjon }.joinToString())
            .replace("@ingenRefusjon", timeseries.map { it.ingenRefusjon }.joinToString())

            .replace("@lpsAntallIM", filteredLpsStats.map { """{value: ${it.antallInntektsmeldinger}, name: "${it.lpsNavn}"},""" }.joinToString())
            .replace("@lpsAntallVersjoner", filteredLpsStats.map { """{value: ${it.antallVersjoner}, name: "${it.lpsNavn}"},""" }.joinToString())

            .replace("@arsak", arsakStats.map { """"${it.arsak}"""" }.joinToString())
            .replace("@begrunnelseAntall", arsakStats.map { it.antall }.joinToString())

        runBlocking {
            val response = httpClient.put<HttpResponse>("$datapakkeApiUrl/$datapakkeId") {
                body = populatedDatapakke
            }

            logger.info("Oppdaterte datapakke $datapakkeId med respons ${response.readText()}")
        }
    }
}
