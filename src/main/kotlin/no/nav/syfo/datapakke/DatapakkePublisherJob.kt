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
import no.nav.syfo.repository.InntektsmeldingRepository
import java.time.Duration

class DatapakkePublisherJob(
    private val imRepo: IMStatsRepo,
    private val httpClient: HttpClient,
    private val datapakkeApiUrl: String,
    private val datapakkeId: String,
) :
    RecurringJob(
        CoroutineScope(Dispatchers.IO),
        Duration.ofHours( 24).toMillis()
    ) {

    override fun doJob() {
        val datapakkeTemplate = "datapakke/datapakke-spinn.json".loadFromResources()
        val timeseries = imRepo.getWeeklyStats().sortedBy { it.weekNumber }
        val lpsStats = imRepo.getLPSStats()
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

            .replace("@lpsNavn", lpsStats.joinToString { """"${it.lpsNavn}"""" })
            .replace("@lpsAntallIM", lpsStats.map { it.antallInntektsmeldinger }.joinToString())
            .replace("@lpsAntallVersjoner", lpsStats.map { it.antallVersjoner }.joinToString())

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
