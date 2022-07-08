package no.nav.syfo.client.saf

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.util.toByteArray
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helsearbeidsgiver.utils.MdcUtils
import no.nav.helsearbeidsgiver.utils.logger

/**
 * REST tjeneste for å hente fysisk dokument fra arkivet.
 *
 * https://confluence.adeo.no/display/BOA/saf+-+REST+hentdokument
 */
class SafDokumentClient constructor(
    private val url: String,
    private val httpClient: HttpClient,
    private val stsClient: AccessTokenProvider
) {
    private val logger = this.logger()

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String
    ): ByteArray? {
        logger.info("Henter dokument fra journalpostId $journalpostId, og dokumentInfoId $dokumentInfoId")
        val response = runBlocking {
            httpClient.get<HttpStatement>("$url/hentdokument/$journalpostId/$dokumentInfoId/ORIGINAL") {
                accept(ContentType.Application.Xml)
                header("Authorization", "Bearer ${stsClient.getToken()}")
                header("Nav-Callid", MdcUtils.getCallId())
                header("Nav-Consumer-Id", "syfoinntektsmelding")
            }.execute()
        }
        if (response.status != HttpStatusCode.OK) {
            logger.info("Saf returnerte: httpstatus {}", response.status)
            return null
        }
        return runBlocking {
            response.content.toByteArray()
        }
    }
}
