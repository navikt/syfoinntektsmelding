package no.nav.syfo.client.saf

import io.ktor.http.*
import io.ktor.util.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.client.buildHttpClientJson
import no.nav.syfo.client.saf.model.JournalResponse
import no.nav.syfo.domain.JournalStatus
import no.nav.syfo.koin.buildObjectMapper
import org.assertj.core.api.Assertions
import org.junit.Test

class SafJournalpostClientTest {

    private val stsClient = mockk<AccessTokenProvider>(relaxed = true)

    val objectMapper = buildObjectMapper()

    @KtorExperimentalAPI
    private lateinit var client: SafJournalpostClient

    @Test
    fun `Skal hente ut gyldig respons`() {
        client = SafJournalpostClient(buildHttpClientJson(HttpStatusCode.OK, gyldig()), "http://localhost", stsClient )
        runBlocking {
            val journalResponse = client.getJournalpostMetadata("123")
            Assertions.assertThat(journalResponse.errors).isNull()
            Assertions.assertThat(journalResponse.data).isNotNull()
            Assertions.assertThat(journalResponse.data?.journalpost?.journalstatus).isEqualTo(JournalStatus.MIDLERTIDIG)
            Assertions.assertThat(journalResponse.data?.journalpost?.dokumenter!![0].dokumentInfoId).isEqualTo("abc")
        }
    }

    @Test
    fun `Skal håndtere feil`() {
        client = SafJournalpostClient(buildHttpClientJson(HttpStatusCode.OK, feil()), "http://localhost", stsClient )
        runBlocking {
            val journalResponse = client.getJournalpostMetadata("123")
            Assertions.assertThat(journalResponse.errors).isNotNull()
            Assertions.assertThat(journalResponse.errors!!.size).isEqualTo(1)
            Assertions.assertThat(journalResponse.errors!![0].message).isNotBlank()
        }
    }

    fun getResource(path: String): String {
        return object {}.javaClass.getResource(path).readText()
    }

    fun getJsonFile(path: String): JournalResponse {
        return objectMapper.readValue(getResource(path), JournalResponse::class.java)
    }

    fun gyldig(): JournalResponse {
        return getJsonFile("/saf_gyldig.json")
    }

    fun feil(): JournalResponse {
        return getJsonFile("/saf_error.json")
    }

    @Test
    fun skal_lese_gyldig(){
        gyldig()
    }

    @Test
    fun skal_lese_error(){
        feil()
    }

}
