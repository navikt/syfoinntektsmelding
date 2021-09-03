package no.nav.syfo.client.saf

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import log
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.client.saf.model.GetJournalpostRequest
import no.nav.syfo.client.saf.model.GetJournalpostVariables

fun lagQuery(journalpostId: String) : String {
    return """
            journalpost(journalpostId: $journalpostId) {
                journalstatus,
                datoOpprettet,
                dokumenter {
                  dokumentInfoId
                }
        }"""
}

class SafJournalpostClient(
    private val httpClient: HttpClient,
    private val basePath: String,
    private val stsClient: AccessTokenProvider
) {
    val log = log()

    fun getJournalpostMetadata(journalpostId: String): SafJournalResponse {
        val token = stsClient.getToken()
        log.info("Henter journalpostmetadata for $journalpostId with token size " + token.length)
        val response = runBlocking {
            httpClient.post<SafJournalResponse>(basePath) {
                body = GetJournalpostRequest(query = lagQuery(journalpostId), variables = GetJournalpostVariables(journalpostId))
                header("Authorization", "Bearer $token")
                header("X-Correlation-ID", journalpostId)
                header(HttpHeaders.ContentType, "application/json")
            }
        }
        return response
    }
}
