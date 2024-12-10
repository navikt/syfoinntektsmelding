package no.nav.syfo.client.saf

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger

/**
 * REST tjeneste for å hente fysisk dokument fra arkivet.
 *
 * https://confluence.adeo.no/display/BOA/saf+-+REST+hentdokument
 */
class SafDokumentClient(
    private val url: String,
    private val httpClient: HttpClient,
    private val getAccessToken: () -> String,
) {
    private val logger = this.logger()

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray? {
        logger.info("Henter dokument fra journalpostId $journalpostId, og dokumentInfoId $dokumentInfoId")
        val response =
            runBlocking {
                httpClient.get("$url/hentdokument/$journalpostId/$dokumentInfoId/ORIGINAL") {
                    accept(ContentType.Application.Xml)
                    header("Authorization", "Bearer ${getAccessToken()}")
                    header("Nav-Callid", MdcUtils.getCallId())
                    header("Nav-Consumer-Id", "syfoinntektsmelding")
                }
            }
        if (response.status != HttpStatusCode.OK) {
            logger.info("Saf returnerte: httpstatus {}", response.status)
            return null
        }
        return runBlocking {
            response.call.response.body<ByteArray>()
        }
    }
}
