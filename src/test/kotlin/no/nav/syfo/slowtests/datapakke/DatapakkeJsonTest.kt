package no.nav.syfo.slowtests.datapakke

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.security.mock.oauth2.http.json
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DatapakkeJsonTest : SystemTestBase(){

    val testData = "datapakke/datapakke.json".loadFromResources()

    @Test
    fun `sjekk at object mapper gir gyldig json`() {

        val result = objectMapper.readTree(testData)
        assertTrue(result.isObject)
        assertNotEquals(testData, result)
        assertEquals("\"Inntektsmelding sykepenger\"", result.get("title").toString())
    }
}
