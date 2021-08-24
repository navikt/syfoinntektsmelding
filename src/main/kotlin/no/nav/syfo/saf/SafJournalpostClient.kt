package no.nav.syfo.saf

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpStatement
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.syfo.graphql.model.GraphQLResponse
import log
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.saf.model.GetJournalpostRequest
import no.nav.syfo.saf.model.GetJournalpostVariables
import no.nav.syfo.saf.model.JournalpostResponse

class SafJournalpostClient(
    private val httpClient: HttpClient,
    private val basePath: String,
    private val stsClient: AccessTokenProvider
) {
    val log = log()

    fun lagQuery(journalpostId: String) : String {
        return """
            journalpost(journalpostId: $journalpostId) {
                dokumenter {
                    dokumentInfoId,
                    mottattDato,
                    journalstatus
                }
        }"""
    }

    suspend fun getJournalpostMetadata(journalpostId: String): GraphQLResponse<JournalpostResponse>? {
        log.info("Henter journalpostmetadata for $journalpostId")
        val getJournalpostRequest = GetJournalpostRequest(query = lagQuery(journalpostId), variables = GetJournalpostVariables(journalpostId))
        val httpResponse = httpClient.post<HttpStatement>(basePath) {
            body = getJournalpostRequest
            header(HttpHeaders.Authorization, "Bearer ${stsClient.getToken()}")
            header("X-Correlation-ID", journalpostId)
            header(HttpHeaders.ContentType, "application/json")
        }.execute()

        return when (httpResponse.status) {
            HttpStatusCode.OK -> {
                httpResponse.call.response.receive()
            }
            else -> {
                log.error("SAF svarte noe annet enn OK ${httpResponse.call.response.status} ${httpResponse.call.response.content}")
                null
            }
        }
    }
}
