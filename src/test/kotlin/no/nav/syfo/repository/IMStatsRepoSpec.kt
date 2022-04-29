package no.nav.syfo.repository

import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class IMStatsRepoSpec : SystemTestBase() {

    lateinit var repo: IMStatsRepo

    @BeforeAll
    internal fun setUp() {

        val ds = HikariDataSource(createTestHikariConfig())

        repo = IMStatsRepoImpl(ds)
    }

    @Test
    internal fun `Test getWeeklyStats`() {
        assertDoesNotThrow { repo.getWeeklyStats() }
    }

    @Test
    internal fun `Test getLPSStats`() {
        assertDoesNotThrow { repo.getLPSStats() }
    }

    @Test
    internal fun `Test getArsakStats`() {
        assertDoesNotThrow { repo.getArsakStats() }
    }

    @Test
    internal fun `Test getWeeklyQualityStats`() {
        assertDoesNotThrow { repo.getWeeklyQualityStats() }
    }

    @Test
    internal fun `Test getLPgetFeilFFPerLPSSStats`() {
        assertDoesNotThrow { repo.getLPSStats() }
    }

    @Test
    internal fun `Test getIngenFravaerPerLPS`() {
        assertDoesNotThrow { repo.getLPSStats() }
    }

    @Test
    internal fun `Test getBackToBackPerLPS`() {
        assertDoesNotThrow { repo.getBackToBackPerLPS() }
    }

    @Test
    internal fun `Test getForsinkelseStats`() {
        assertDoesNotThrow { repo.getForsinkelseStats() }
    }

    @Test
    internal fun `Test getOppgaveStats`() {
        assertDoesNotThrow { repo.getOppgaveStats() }
    }

    @Test
    internal fun `Test getForsinkelseWeeklyStats`() {
        assertDoesNotThrow { repo.getForsinkelseWeeklyStats() }
    }
}
