package no.nav.syfo.client.saf

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.client.saf.model.GetJournalpostRequest
import no.nav.syfo.client.saf.model.JournalResponse
import no.nav.syfo.client.saf.model.Journalpost

class SafJournalpostClient(
    private val httpClient: HttpClient,
    private val basePath: String,
    private val getAccessToken: () -> String,
) {
    private val logger = this.logger()

    fun getJournalpostMetadata(journalpostId: String): Journalpost? {
        val accessToken = getAccessToken()
        logger.info("Henter journalpostmetadata for $journalpostId with token size " + accessToken.length)
        return runBlocking {
            try {
                val response: JournalResponse =
                    httpClient
                        .post(basePath) {
                            contentType(ContentType.Application.Json)
                            header("Authorization", "Bearer $accessToken")
                            header("X-Correlation-ID", journalpostId)
                            setBody(GetJournalpostRequest(query = lagQuery(journalpostId)))
                        }.call.response
                        .body<JournalResponse>()
                if (response.errors != null && response.errors.isNotEmpty()) {
                    throw ErrorException(journalpostId, response.errors.toString())
                }
                if (response.data?.journalpost == null) {
                    throw EmptyException(journalpostId)
                }
                response.data.journalpost
            } catch (e: ClientRequestException) {
                when (e.response.status) {
                    HttpStatusCode.Unauthorized -> throw NotAuthorizedException(journalpostId)
                    else -> throw ErrorException(journalpostId, e.response.body())
                }
            }
        }
    }
}

open class SafJournalpostException(
    journalpostId: String,
) : Exception(journalpostId)

open class NotAuthorizedException(
    journalpostId: String,
) : SafJournalpostException(
        "SAF ga ikke tilgang til å lese ut journalpost '$journalpostId'",
    )

open class ErrorException(
    journalpostId: String,
    errors: String,
) : SafJournalpostException(
        "SAF returnerte feil journalpost '$journalpostId': $errors",
    )

open class EmptyException(
    journalpostId: String,
) : SafJournalpostException("SAF returnerte tom journalpost '$journalpostId'")
