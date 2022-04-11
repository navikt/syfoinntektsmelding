package no.nav.syfo.slowtests.datapakke

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.utils.loadFromResources
import no.nav.syfo.slowtests.SystemTestBase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatapakkeJsonTest : SystemTestBase() {

    val testData = "datapakke/datapakke.json".loadFromResources()

    fun buildObjectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule())
            registerModule(KotlinModule())
            registerModule(Jdk8Module())
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            configure(SerializationFeature.INDENT_OUTPUT, true)
            configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setDefaultPrettyPrinter(
                DefaultPrettyPrinter().apply {
                    indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                    indentObjectsWith(DefaultIndenter("  ", "\n"))
                }
            )
        }
    }

    @Test
    fun `sjekk at object mapper gir gyldig json`() {
        val result = buildObjectMapper().readTree(testData)
        assertTrue(result.isObject)
        // assertNotEquals(testData, result) // TODO Denne sammenlikningen må fikses. Gustav
        assertEquals("\"Inntektsmelding sykepenger\"", result.get("title").toString())
    }

    @Test
    @Disabled
    fun `prøv å sende request`() {
        runBlocking {
            httpClient.put<HttpResponse>("https://datakatalog-api.dev.intern.nav.no/v1/datapackage/fb74c8d14d9c579e05b0b4b587843e6b") {
                contentType(ContentType.Application.Json)
                body = buildObjectMapper().readTree(testData)
            }
        }
    }
}
