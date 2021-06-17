package no.nav.syfo.datapakke

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Exploratory")
internal class DatapakkePublisherJobTest {
    @Test
    internal fun publish() {
        val timeseries = imRepo.getWeeklyStats().sortedBy { it.weekNumber }
        val lpsStats = imRepo.getLPSStats()
        val arsakStats = imRepo.getArsakStats()

    }
}
