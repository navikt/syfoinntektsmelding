package no.nav.syfo.slowtests.datapakke

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.koin.buildObjectMapper
import no.nav.syfo.repository.ArsakStats
import no.nav.syfo.repository.ForsinkelseStats
import no.nav.syfo.repository.ForsinkelseWeeklyStats
import no.nav.syfo.repository.IMStatsRepo
import no.nav.syfo.repository.IMWeeklyQualityStats
import no.nav.syfo.repository.IMWeeklyStats
import no.nav.syfo.repository.LPSStats
import no.nav.syfo.repository.OppgaveStats
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class DatapakkePublisherJobSpec : SystemTestBase() {

    val repo = mockk<IMStatsRepo>()

    @BeforeAll
    internal fun setUp() {
        val weeks = (7..52) + (1..6)

        every { repo.getWeeklyStats() } returns weeks
            .map {
                IMWeeklyStats(
                    it,
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                )
            }.toList()

        every { repo.getArsakStats() } returns (1..10)
            .map {
                ArsakStats(
                    UUID.randomUUID().toString().substring(0, 5),
                    Random.nextInt(500)
                )
            }.toList()

        every { repo.getLPSStats() } returns listOf(
            LPSStats(
                "SAP [SID:QHR/002][BUILD:20210820]",
                1,
                6
            ),
            LPSStats(
                "Aditro Lønn",
                2,
                3
            ),
            LPSStats(
                "24SevenOffice",
                1,
                25
            )
        )

        every { repo.getWeeklyQualityStats() } returns weeks
            .map {
                IMWeeklyQualityStats(
                    it,
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                    Random.nextInt(10000),
                )
            }.toList()

        every { repo.getFeilFFPerLPS() } returns (1..10)
            .map {
                LPSStats(
                    UUID.randomUUID().toString().substring(0, 5),
                    Random.nextInt(50),
                    Random.nextInt(5000)
                )
            }.toList()

        every { repo.getIngenFravaerPerLPS() } returns (1..10)
            .map {
                LPSStats(
                    UUID.randomUUID().toString().substring(0, 5),
                    Random.nextInt(50),
                    Random.nextInt(5000)
                )
            }.toList()

        every { repo.getBackToBackPerLPS() } returns (1..10)
            .map {
                LPSStats(
                    UUID.randomUUID().toString().substring(0, 5),
                    Random.nextInt(50),
                    Random.nextInt(5000)
                )
            }.toList()

        every { repo.getForsinkelseStats() } returns (1..30)
            .map {
                ForsinkelseStats(
                    Random.nextInt(50),
                    Random.nextInt(50),
                    Random.nextInt(160)
                )
            }.toList()

        every { repo.getOppgaveStats() } returns (1..10)
            .map {
                OppgaveStats(
                    Random.nextInt(100),
                    Random.nextInt(100),
                    Random.nextInt(100),
                    Random.nextInt(100),
                    Random.nextInt(100),
                    Random.nextInt(100),
                    Random.nextInt(100),
                    "2021-08-$it"
                )
            }.toList()

        val list = mutableListOf<ForsinkelseWeeklyStats>()
        (42..52).map { uke ->
            (0..3).map { bucket ->
                list.add(ForsinkelseWeeklyStats(Random.nextInt(100), Random.nextInt(100), bucket, uke, 2021))
            }
        }
        (1..10).map { uke ->
            (0..3).map { bucket ->
                list.add(ForsinkelseWeeklyStats(Random.nextInt(100), Random.nextInt(100), bucket, uke, 2022))
            }
        }
        list.remove(list.filter { it.bucket == 1 }.filter { it.uke == 3 }[0])
        list.remove(list.filter { it.bucket == 2 }.filter { it.uke == 6 }[0])
        every { repo.getForsinkelseWeeklyStats() } returns list
    }

    @Test
    @Disabled
    internal fun name() = suspendableTest {
        val om = buildObjectMapper()

        DatapakkePublisherJob(
            repo,
            httpClient,
            "https://datakatalog-api.dev.intern.nav.no/v1/datapackage",
            "fb74c8d14d9c579e05b0b4b587843e6b",
            om = om
        ).doJob()
    }
}
