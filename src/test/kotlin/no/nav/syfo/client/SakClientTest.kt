package no.nav.syfo.client

import io.ktor.http.HttpStatusCode
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SakClientTest {

    private val tokenConsumer = mockk<TokenConsumer>(relaxed = true)

    private lateinit var sakClient: SakClient

    val RESPONSE_EXAMPLE = "{\"id\":1, \"tema\":\"tema\", \"aktoerId\":\"aktør-id\", \"orgnr\":\"orgnr\", \"fagsakNr\":\"faksak-nr\", \"applikasjon\":\"app\", \"opprettetAv\":\"av\", \"opprettetTidspunkt\": \"2007-12-03T10:15:30+01:00\"}"

    @Test
    @Disabled
    fun `Skal opprette sak`() {
        sakClient = SakClient("http://localhost", tokenConsumer, buildHttpClientJson(HttpStatusCode.OK, RESPONSE_EXAMPLE))
        runBlocking {
            val response = sakClient.opprettSak("1234", "msgid")
            Assertions.assertThat(response.id).isEqualTo(1)
        }
    }

    @Test
    fun `Skal håndtere feil`() {
        sakClient = SakClient("http://localhost", tokenConsumer, buildHttpClientText(HttpStatusCode.BadRequest, ""))
        runBlocking {
            assertThrows<VerifyError> {
                sakClient.opprettSak("1234", "msgid")
            }
        }
    }
}
