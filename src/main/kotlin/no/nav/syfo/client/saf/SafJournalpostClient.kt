package no.nav.syfo.client.saf

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import log
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.graphql.model.SafJournalResponse
import no.nav.syfo.client.saf.model.GetJournalpostRequest
import no.nav.syfo.client.saf.model.GetJournalpostVariables

class SafJournalpostClient(
    private val httpClient: HttpClient,
    private val basePath: String,
    private val stsClient: AccessTokenProvider
) {
    val log = log()

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

    fun getJournalpostMetadata(journalpostId: String): SafJournalResponse {
        log.info("Henter journalpostmetadata for $journalpostId")
        val response = runBlocking {
            httpClient.post<SafJournalResponse>(basePath) {
                body = GetJournalpostRequest(query = lagQuery(journalpostId), variables = GetJournalpostVariables(journalpostId))
                header(HttpHeaders.Authorization, "Bearer ${stsClient.getToken()}")
                header("X-Correlation-ID", journalpostId)
                header(HttpHeaders.ContentType, "application/json")
            }
        }
        return response
    }
}
