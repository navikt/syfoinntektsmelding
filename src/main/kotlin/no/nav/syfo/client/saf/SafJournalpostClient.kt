package no.nav.syfo.client.saf

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.*
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import log
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.client.saf.model.GetJournalpostRequest
import no.nav.syfo.client.saf.model.GetJournalpostVariables
import no.nav.syfo.client.saf.model.JournalResponse

class SafJournalpostClient(
    private val httpClient: HttpClient,
    private val basePath: String,
    private val stsClient: AccessTokenProvider
) {
    val log = log()

    fun getJournalpostMetadata(journalpostId: String): JournalResponse {
        val token = stsClient.getToken()
        log.info("Henter journalpostmetadata for $journalpostId with token size " + token.length)
        return runBlocking {
            httpClient.post<JournalResponse>(basePath) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                header("X-Correlation-ID", journalpostId)
                body = GetJournalpostRequest(query = lagQuery(journalpostId), variables = GetJournalpostVariables(journalpostId))
            }
        }
    }
}
