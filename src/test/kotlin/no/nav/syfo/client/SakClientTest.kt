package no.nav.syfo.client

import io.ktor.http.HttpStatusCode
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class SakClientTest {

    private val tokenConsumer = mockk<TokenConsumer>(relaxed = true)
    val RESPONSE_EXAMPLE = SakResponse(
        1, "tema", "aktør-id", "orgnr", "fagsakNr", "app", "av",
        ZonedDateTime.of(2007, 12, 3, 15, 0, 0, 0, ZoneId.of("UTC+1"))
    )

    @Test
    fun `Skal opprette sak`() {
        val sakClient = SakClient("http://localhost", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, RESPONSE_EXAMPLE))
        val response = runBlocking {
            sakClient.opprettSak("1234", "msgid")
        }
        assertEquals(1, response.id)
    }

    @Test
    fun `Skal håndtere feil`() {
        val sakClient = SakClient("http://localhost", tokenConsumer, buildHttpClientJson(HttpStatusCode.BadRequest, RESPONSE_EXAMPLE))
        assertThrows<Exception> {
            runBlocking {
                sakClient.opprettSak("1234", "msgid")
            }
        }
    }
}
