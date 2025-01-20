package no.nav.syfo.client.dokarkiv

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.buildHttpClientText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DokArkivClientTest {
    private lateinit var dokArkivClient: DokArkivClient

    @Test
    fun `Skal ferdigstille journalpost når man får status OK`() {
        dokArkivClient = DokArkivClient("", buildHttpClientText(HttpStatusCode.OK, ""), ::mockAccessToken)
        runBlocking {
            val resultat = dokArkivClient.ferdigstillJournalpost("111", "1001")
            assertEquals("", resultat)
        }
    }

    @Test
    fun `Skal håndtere at ferdigstilling av journalpost feiler`() {
        dokArkivClient = DokArkivClient("", buildHttpClientText(HttpStatusCode.InternalServerError, ""), ::mockAccessToken)
        runBlocking {
            assertThrows<Exception> {
                dokArkivClient.ferdigstillJournalpost("111", "1001")
            }
        }
    }

    @Test
    fun `Skal oppdatere journalpost når man får status OK`() {
        dokArkivClient = DokArkivClient("", buildHttpClientText(HttpStatusCode.OK), ::mockAccessToken)
        runBlocking {
            val req = mapOppdaterRequest("123")
            val resultat = dokArkivClient.oppdaterJournalpost("111", req, "")
            assertEquals(HttpStatusCode.OK, resultat)
        }
    }

    @Test
    fun `Skal oppdatere journalpost med feilregistrering`() {
        dokArkivClient = DokArkivClient("", buildHttpClientText(HttpStatusCode.OK), ::mockAccessToken)
        runBlocking {
            val req = mapFeilregistrertRequest("123", "dok123")
            val resultat = dokArkivClient.oppdaterJournalpost("111", req, "")
            assertEquals(HttpStatusCode.OK, resultat)
        }
    }

    @Disabled
    @Test
    fun `Skal håndtere at oppdatering av journalpost feiler`() {
        dokArkivClient = DokArkivClient("", buildHttpClientText(HttpStatusCode.InternalServerError, ""), ::mockAccessToken)
        val req = mapOppdaterRequest("123")
        runBlocking {
            assertThrows<Exception> {
                dokArkivClient.oppdaterJournalpost("111", req, "")
            }
        }
    }
}

private fun mockAccessToken(): String = "mock dokarkiv token"
