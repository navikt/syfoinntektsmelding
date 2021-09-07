package no.nav.syfo.slowtests.datapakke

import io.mockk.every
import io.mockk.mockk
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.repository.ArsakStats
import no.nav.syfo.repository.IMStatsRepo
import no.nav.syfo.repository.IMWeeklyStats
import no.nav.syfo.repository.LPSStats
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
    }

    @Test
    @Disabled
    internal fun name() = suspendableTest {
        DatapakkePublisherJob(
            repo,
            httpClient,
            "https://datakatalog-api.dev.intern.nav.no/v1/datapackage",
            "8f29efc4b7e41002130db5a172587fd4",
            om = objectMapper
        ).doJob()
    }
}
