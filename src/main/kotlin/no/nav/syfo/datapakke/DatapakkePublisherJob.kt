package no.nav.syfo.datapakke

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.syfo.repository.IMStatsRepo
import no.nav.syfo.util.DatapakkeUtil
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

class DatapakkePublisherJob(
    private val imRepo: IMStatsRepo,
    private val httpClient: HttpClient,
    private val datapakkeApiUrl: String,
    private val datapakkeId: String,
    private val applyWeeklyOnly: Boolean = false,
    private val logDatapakke: Boolean = false
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

        val sapList = lpsStats.filter { it.lpsNavn.startsWith("SAP") }
        val mergedSapStats = DatapakkeUtil.countSAP(sapList)

        val filteredLpsStats = lpsStats // Altinn er ikke en LPS, og fjerner alle SAP duplikatene
            .filter { !it.lpsNavn.startsWith("Altinn") }
            .filter { !it.lpsNavn.startsWith("SAP") }
            .toMutableList()

        mergedSapStats?.let { filteredLpsStats.add(it) }

        val arsakStats = imRepo.getArsakStats()

        val timeseriesKS = imRepo.getWeeklyQualityStats().sortedBy { it.weekNumber }

        val lpsFeilFF = imRepo.getFeilFFPerLPS()
            .filter { !it.lpsNavn.startsWith("Altinn") }
            .filter { !it.lpsNavn.startsWith("SAP") }
            .toMutableList()

        val lpsIngenFravaer = imRepo.getIngenFravaerPerLPS()
            .filter { !it.lpsNavn.startsWith("Altinn") }
            .filter { !it.lpsNavn.startsWith("SAP") }
            .toMutableList()

        val lpsBackToBack = imRepo.getBackToBackPerLPS()
            .filter { !it.lpsNavn.startsWith("Altinn") }
            .filter { !it.lpsNavn.startsWith("SAP") }
            .toMutableList()

        val forsinket = imRepo.getForsinkelseStats()

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

            .replace("@lpsAntallIM", filteredLpsStats.map { //language=JSON
                """{"value": ${it.antallInntektsmeldinger}, "name": "${it.lpsNavn}"}""" }.joinToString())

            .replace("@lpsAntallVersjoner", filteredLpsStats.map { //language=JSON
                """{"value": ${it.antallVersjoner}, "name": "${it.lpsNavn}"}""" }.joinToString())

            .replace("@arsak", arsakStats
                .filter { it.arsak.isNotBlank() }
                .map { //language=JSON
                """{"value": ${it.antall}, "name": "${it.arsak}"}""" }.joinToString())

            .replace("@KSukeSerie", timeseriesKS.map { it.weekNumber }.joinToString())
            .replace("@KStotal", timeseriesKS.map { it.total }.joinToString())
            .replace("@KSingenArbeid", timeseriesKS.map { it.ingen_arbeidsforhold_id }.joinToString())
            .replace("@KSharArbeid", timeseriesKS.map { it.har_arbeidsforhold_id }.joinToString())
            .replace("@KSenPeriode", timeseriesKS.map { it.en_periode }.joinToString())
            .replace("@KStoPerioder", timeseriesKS.map { it.to_perioder }.joinToString())
            .replace("@KSoverToPerioder", timeseriesKS.map { it.over_to_perioder }.joinToString())
            .replace("@KSriktigFF", timeseriesKS.map { it.riktig_ff }.joinToString())
            .replace("@KSfeilFF", timeseriesKS.map { it.feil_ff }.joinToString())
            .replace("@KSikkeFravaerUtenRef", timeseriesKS.map { it.ingen_fravaer }.joinToString())
            .replace("@KSikkeFravaerMedRef", timeseriesKS.map { it.ingen_fravaer_med_refusjon }.joinToString())

            .replace("@KSlpsAntallFeilFF", lpsFeilFF.map { //language=JSON
                """{"value": ${it.antallInntektsmeldinger}, "name": "${it.lpsNavn}"}""" }.joinToString())


            .replace("@KSlpsAntallNullFra", lpsIngenFravaer.map { //language=JSON
                """{"value": ${it.antallInntektsmeldinger}, "name": "${it.lpsNavn}"}""" }.joinToString())

            .replace("@KSlpsAntallVersjonerNullFra", lpsIngenFravaer.map { //language=JSON
                """{"value": ${it.antallVersjoner}, "name": "${it.lpsNavn}"}""" }.joinToString())

            .replace("@KSlpsAntallBackToBack", lpsBackToBack.map { //language=JSON
                """{"value": ${it.antallInntektsmeldinger}, "name": "${it.lpsNavn}"}""" }.joinToString())


            .replace("@KSForsinketData", forsinket.map { //language=JSON
                """[${it.antall_med_forsinkelsen_altinn},${it.antall_med_forsinkelsen_lps},${it.dager_etter_ff}]"""
            }.joinToString())

        runBlocking {
            if (logDatapakke) logger.info("Datapakke $datapakkeId med innhold: $populatedDatapakke")

            val response = httpClient.put<HttpResponse>("$datapakkeApiUrl/$datapakkeId") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = populatedDatapakke
            }

            logger.info("Oppdaterte datapakke $datapakkeId med respons ${response.readText()}")
        }
    }
}
