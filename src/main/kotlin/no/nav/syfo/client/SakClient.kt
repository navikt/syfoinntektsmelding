package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import net.logstash.logback.argument.StructuredArguments.fields
import no.nav.syfo.LoggingMeta
import no.nav.syfo.helpers.retry
import log
import java.time.LocalDate
import java.time.ZonedDateTime

@KtorExperimentalAPI
class SakClient constructor(
        private val url: String,
        private val oidcClient: StsOidcClient,
        private val httpClient: HttpClient
) {

    private val log = log()

    private suspend fun opprettSak(pasientAktoerId: String, msgId: String): SakResponse = retry("opprett_sak") {
        httpClient.post<SakResponse>(url) {
            contentType(ContentType.Application.Json)
            header("X-Correlation-ID", msgId)
            header("Authorization", "Bearer ${oidcClient.oidcToken().access_token}")
            body = OpprettSakRequest(
                    tema = "SYM",
                    applikasjon = "FS22",
                    aktoerId = pasientAktoerId,
                    orgnr = null,
                    fagsakNr = null
            )
        }
    }

    private suspend fun finnSak(pasientAktoerId: String, msgId: String): List<SakResponse>? = retry("finn_sak") {
        httpClient.get<List<SakResponse>?>(url) {
            contentType(ContentType.Application.Json)
            header("X-Correlation-ID", msgId)
            header("Authorization", "Bearer ${oidcClient.oidcToken().access_token}")
            parameter("tema", "SYM")
            parameter("aktoerId", pasientAktoerId)
            parameter("applikasjon", "FS22")
        }
    }

    suspend fun finnEllerOpprettSak(sykmeldingsId: String, aktorId: String, loggingMeta: LoggingMeta): String {
        val finnSakRespons = finnSak(aktorId, sykmeldingsId)

        val sakIdFraRespons = finnSakRespons?.sortedBy { it.opprettetTidspunkt }?.lastOrNull()?.id?.toString()
        return if (sakIdFraRespons == null) {
            val opprettSakRespons = opprettSak(aktorId, sykmeldingsId)
            log.info("Opprettet en sak med sakid {}, {}", opprettSakRespons.id.toString(), fields(loggingMeta))

            opprettSakRespons.id.toString()
        } else {
            log.info("Fant en sak med sakid {}, {}", sakIdFraRespons, fields(loggingMeta))
            sakIdFraRespons
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
