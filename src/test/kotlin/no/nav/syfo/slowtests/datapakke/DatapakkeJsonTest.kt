package no.nav.syfo.slowtests.datapakke

import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatapakkeJsonTest : SystemTestBase() {

    val testData = "datapakke/datapakke.json".loadFromResources()

    @Test
    fun `sjekk at object mapper gir gyldig json`() {
        val result = objectMapper.readTree(testData)
        assertTrue(result.isObject)
        // assertNotEquals(testData, result) // TODO Denne sammenlikningen må fikses. Gustav
        assertEquals("\"Inntektsmelding sykepenger\"", result.get("title").toString())
    }

    @Test
    @Disabled
    fun `prøv å sende request`() {
        runBlocking {
            val response = httpClient.put<HttpResponse>("https://datakatalog-api.dev.intern.nav.no/v1/datapackage/fb74c8d14d9c579e05b0b4b587843e6b") {
                contentType(ContentType.Application.Json)
                body = objectMapper.readTree(testData)
            }
        }
    }
}
