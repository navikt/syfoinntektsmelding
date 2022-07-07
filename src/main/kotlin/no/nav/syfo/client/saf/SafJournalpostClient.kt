package no.nav.syfo.client.saf

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helse.arbeidsgiver.utils.logger
import no.nav.syfo.client.saf.model.GetJournalpostRequest
import no.nav.syfo.client.saf.model.JournalResponse
import no.nav.syfo.client.saf.model.Journalpost

class SafJournalpostClient(
    private val httpClient: HttpClient,
    private val basePath: String,
    private val stsClient: AccessTokenProvider
) {
    private val logger = this.logger()

    fun getJournalpostMetadata(journalpostId: String): Journalpost? {
        val token = stsClient.getToken()
        logger.info("Henter journalpostmetadata for $journalpostId with token size " + token.length)
        val response = runBlocking {
            httpClient.post<JournalResponse>(basePath) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                header("X-Correlation-ID", journalpostId)
                body = GetJournalpostRequest(query = lagQuery(journalpostId))
            }
        }
        if (response.status == 401) {
            throw NotAuthorizedException(journalpostId)
        }
        if (response.errors != null && response.errors.isNotEmpty()) {
            throw ErrorException(journalpostId, response.errors.toString())
        }
        return response.data!!.journalpost
    }
}

open class SafJournalpostException(journalpostId: String) : Exception(journalpostId)

open class NotAuthorizedException(journalpostId: String) : SafJournalpostException("SAF ga ikke tilgang til å lese ut journalpost '$journalpostId'")
open class ErrorException(journalpostId: String, errors: String) : SafJournalpostException("SAF returnerte feil journalpost '$journalpostId': $errors")
open class EmptyException(journalpostId: String) : SafJournalpostException("SAF returnerte tom journalpost '$journalpostId'")
