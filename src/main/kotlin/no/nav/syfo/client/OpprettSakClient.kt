package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.ZonedDateTime

class SakClient constructor(
    val opprettsakUrl: String,
    val tokenConsumer: TokenConsumer,
    val httpClient: HttpClient
) {

    suspend fun opprettSak(pasientAktoerId: String, msgId: String): SakResponse {
        return httpClient.post(opprettsakUrl) {
            contentType(ContentType.Application.Json)
            header("X-Correlation-ID", msgId)
            header("Authorization", "Bearer ${tokenConsumer.token}")
            body = OpprettSakRequest(
                tema = "SYK",
                applikasjon = "FS22",
                aktoerId = pasientAktoerId,
                orgnr = null,
                fagsakNr = null
            )
        }
    }
}

data class OpprettSakRequest(
    val tema: String,
    val applikasjon: String,
    val aktoerId: String,
    val orgnr: String?,
    val fagsakNr: String?
)

data class SakResponse(
    val id: Long,
    val tema: String,
    val aktoerId: String,
    val orgnr: String?,
    val fagsakNr: String?,
    val applikasjon: String,
    val opprettetAv: String,
    val opprettetTidspunkt: ZonedDateTime
)
