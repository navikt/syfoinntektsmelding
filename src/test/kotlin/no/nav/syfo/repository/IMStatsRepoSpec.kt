package no.nav.syfo.repository

import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals

class IMStatsRepoSpec : SystemTestBase() {

    lateinit var repo: IMStatsRepo

    @BeforeAll
    internal fun setUp() {

        val ds = HikariDataSource(createTestHikariConfig())

        repo = IMStatsRepoImpl(ds)
    }

    @Test
    internal fun `Test getWeeklyStats`() {
        assertEquals(0, repo.getWeeklyStats().size)
    }
    @Test
    internal fun `Test getLPSStats`() {
        assertEquals(0, repo.getLPSStats().size)
    }
    @Test
    internal fun `Test getArsakStats`() {
        assertEquals(0, repo.getArsakStats().size)
    }
    @Test
    internal fun `Test getWeeklyQualityStats`() {
        assertEquals(0, repo.getWeeklyQualityStats().size)
    }
    @Test
    internal fun `Test getLPgetFeilFFPerLPSSStats`() {
        assertEquals(0, repo.getLPSStats().size)
    }
    @Test
    internal fun `Test getIngenFravaerPerLPS`() {
        assertEquals(0, repo.getLPSStats().size)
    }
    @Test
    internal fun `Test getBackToBackPerLPS`() {
        assertEquals(0, repo.getBackToBackPerLPS().size)
    }
    @Test
    internal fun `Test getForsinkelseStats`() {
        assertEquals(0, repo.getForsinkelseStats().size)
    }
    @Test
    internal fun `Test getOppgaveStats`() {
        assertDoesNotThrow { repo.getOppgaveStats() }
    }
    @Test
    internal fun `Test getForsinkelseWeeklyStats`() {
        assertEquals(0, repo.getForsinkelseWeeklyStats().size)
    }
}
