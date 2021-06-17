package no.nav.syfo.slowtests.datapakke

import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.datapakke.DatapakkePublisherJob
import no.nav.syfo.inntektsmeldingEntitet
import no.nav.syfo.repository.IMStatsRepo
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.InntektsmeldingRepositoryImp
import no.nav.syfo.repository.createTestHikariConfig
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.Ignore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.koin.test.inject

class DatapakkePublisherJobTest : SystemTestBase() {

    val repo by inject<IMStatsRepo>()
    val testKrav = inntektsmeldingEntitet
    lateinit var repository: InntektsmeldingRepository

    @BeforeAll
    internal fun setUp() {
        val ds = HikariDataSource(createTestHikariConfig())
        repository = InntektsmeldingRepositoryImp(ds)
        repository.deleteAll()
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Disabled
    @Test
    internal fun name() = suspendableTest {
        val ds = HikariDataSource(createTestHikariConfig())
        val repository = InntektsmeldingRepositoryImp(ds)

        repository.lagreInnteksmelding(testKrav)
        DatapakkePublisherJob(
            repo,
            httpClient,
            "https://datakatalog-api.dev.intern.nav.no/v1/datapackage",
        "8f29efc4b7e41002130db5a172587fd4"
        ).doJob()
    }
}
