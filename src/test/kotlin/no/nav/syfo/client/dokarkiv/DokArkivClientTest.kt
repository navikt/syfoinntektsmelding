package no.nav.syfo.client.dokarkiv

import io.ktor.http.HttpStatusCode
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.client.buildHttpClientText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DokArkivClientTest {

    private val mockStsClient = mockk<AccessTokenProvider>(relaxed = true)

    private lateinit var dokArkivClient: DokArkivClient

    @Test
    fun `Skal ferdigstille journalpost når man får status OK`() {
        dokArkivClient = DokArkivClient("", mockStsClient, buildHttpClientText(HttpStatusCode.OK, ""))
        runBlocking {
            val resultat = dokArkivClient.ferdigstillJournalpost("111", "1001")
            assertEquals("", resultat)
        }
    }

    @Test
    fun `Skal håndtere at ferdigstilling av journalpost feiler`() {
        dokArkivClient = DokArkivClient("", mockStsClient, buildHttpClientText(HttpStatusCode.InternalServerError, ""))
        runBlocking {
            assertThrows<Exception> {
                dokArkivClient.ferdigstillJournalpost("111", "1001")
            }
        }
    }

    @Test
    fun `Skal oppdatere journalpost når man får status OK`() {
        dokArkivClient = DokArkivClient("", mockStsClient, buildHttpClientText(HttpStatusCode.OK))
        runBlocking {
            val resultat = dokArkivClient.oppdaterJournalpost("111", "123", false, "abc123", "Stark industries", "1001")
            assertEquals(HttpStatusCode.OK, resultat.status)
        }
    }

    @Test
    @Disabled
    fun `Skal håndtere at oppdatering av journalpost feiler`() {
        dokArkivClient = DokArkivClient("", mockStsClient, buildHttpClientText(HttpStatusCode.InternalServerError, ""))
        runBlocking {
            assertThrows<Exception> {
                dokArkivClient.oppdaterJournalpost("111", "123", false, "abc123", "Stark industries", "1001")
            }
        }
    }
}
