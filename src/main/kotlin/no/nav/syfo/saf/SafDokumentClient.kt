package no.nav.syfo.saf

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.util.KtorExperimentalAPI
import log
import no.nav.syfo.saf.exception.SafNotFoundException
import no.nav.syfo.util.MDCOperations
import java.util.*

@KtorExperimentalAPI
class SafDokumentClient constructor(
    private val url: String,
    private val httpClient: HttpClient
) {
    val log = log()
     suspend fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
        accessToken: String
    ): ByteArray? {
        log.info("Henter dokument fra journalpostId $journalpostId, og dokumentInfoId $dokumentInfoId")
        val httpResponse =
            httpClient.get<HttpStatement>("$url/hentdokument/$journalpostId/$dokumentInfoId/ARKIV") {
                accept(ContentType.Application.Pdf)
                header("Authorization", "Bearer $accessToken")
                header("Nav-Callid", MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, UUID.randomUUID().toString()))
                header("Nav-Consumer-Id", "syfoinntektsmelding")
            }.execute()
        log.info("Saf returnerte: httpstatus {}", httpResponse.status)
        return when (httpResponse.status) {
            HttpStatusCode.NotFound -> {
                log.error("Dokumentet finnes ikke for journalpostId {}, response {}", journalpostId, httpResponse.call.response.receive())
                throw SafNotFoundException("Saf returnerte: httpstatus ${httpResponse.status}")
            }
            HttpStatusCode.InternalServerError -> {
                log.error("Noe gikk galt ved sjekking av status eller tilgang for journalpostId {},  response {}", journalpostId, httpResponse.call.response.receive())
                throw RuntimeException("Saf returnerte: httpstatus ${httpResponse.status}")
            }
            HttpStatusCode.Forbidden -> {
                log.error("Bruker har ikke tilgang til for journalpostId {}, response {}", journalpostId,  httpResponse.call.response.receive())
                throw RuntimeException("Saf returnerte: httpstatus ${httpResponse.status}")
            }
            HttpStatusCode.Unauthorized -> {
                log.error("Bruker har ikke tilgang til for journalpostId {}, response {}", journalpostId,  httpResponse.call.response.receive())
                throw RuntimeException("Saf returnerte: httpstatus ${httpResponse.status}")
            }
            HttpStatusCode.NotAcceptable -> {
                log.error("Not Acceptable for journalpostId {}, response {}", journalpostId,  httpResponse.call.response.receive())
                throw RuntimeException("Saf returnerte: httpstatus ${httpResponse.status}")
            }
            HttpStatusCode.BadRequest -> {
                log.error("Bad Requests for journalpostId {}, response {}", journalpostId,  httpResponse.call.response.receive())
                throw RuntimeException("Saf returnerte: httpstatus ${httpResponse.status}")
            }
            else -> {
                log.info("Hentet papirsykmelding pdf for journalpostId {}", journalpostId, )
                httpResponse.call.response.receive<ByteArray>()
            }
        }
    }

}
