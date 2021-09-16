package no.nav.syfo.slowtests.datapakke

import io.mockk.every
import io.mockk.mockk
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.repository.*
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random

class DatapakkePublisherJobTest : SystemTestBase() {

    val repo = mockk<IMStatsRepo>()

    @BeforeAll
    internal fun setUp() {
        every { repo.getWeeklyStats() } returns (1..25)
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

        every { repo.getLPSStats() } returns (1..10)
            .map {
                LPSStats(
                    UUID.randomUUID().toString().substring(0, 5),
                    Random.nextInt(50),
                    Random.nextInt(5000)
                )
            }.toList()

        every { repo.getWeeklyQualityStats() } returns (1..25)
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
                    "2021-08-14"
                )
            }.toList()
    }


    @Test
    internal fun name() = suspendableTest {
        DatapakkePublisherJob(
            repo,
            httpClient,
            "localhost:3000",
            "",
            om = objectMapper
        ).doJob()

    }
}
