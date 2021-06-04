package no.nav.helse.grensekomp.datapakke

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.syfo.repository.InntektsmeldingRepository
import java.time.Duration

class DatapakkePublisherJob(
    private val imRepo: InntektsmeldingRepository,
    private val httpClient: HttpClient,
    private val datapakkeApiUrl: String,
    private val datapakkeId: String,
) :
    RecurringJob(
        CoroutineScope(Dispatchers.IO),
        Duration.ofHours(4).toMillis()
    ) {

    override fun doJob() {
        val datapakkeTemplate = "datapakke/datapakke-spinn.json".loadFromResources()
        val stats = imRepo.statsByWeek(gClient.hentGrunnbeløp().grunnbeløp * 6.0)
        val sorted = stats.keys.sorted()

       val populatedDatapakke = datapakkeTemplate
           .replace("@ukeSerie", sorted.joinToString())
           .replace("@antallSerie", sorted.map { stats[it]!!.first }.joinToString())
           .replace("@beløpSerie", sorted.map { stats[it]!!.second }.joinToString())

        runBlocking {
            val response = httpClient.put<HttpResponse>("$datapakkeApiUrl/$datapakkeId") {
                body = populatedDatapakke
            }

            logger.info("Oppdaterte datapakke $datapakkeId med respons ${response.readText()}")
        }
    }
}
