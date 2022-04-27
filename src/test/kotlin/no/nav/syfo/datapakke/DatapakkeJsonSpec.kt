package no.nav.syfo.datapakke

import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatapakkeJsonSpec : SystemTestBase() {

    val testData = "datapakke/datapakke.json".loadFromResources()

    @Test
    fun `sjekk at object mapper gir gyldig json`() {
        val result = objectMapper.readTree(testData)
        assertTrue(result.isObject)
        assertEquals("\"Inntektsmelding sykepenger\"", result.get("title").toString())
    }
}
