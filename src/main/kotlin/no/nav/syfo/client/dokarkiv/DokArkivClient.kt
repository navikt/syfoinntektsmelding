package no.nav.syfo.client.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.helpers.retry
import org.slf4j.LoggerFactory
import java.io.IOException

// NAV-enheten som personen som utfører journalføring jobber for. Ved automatisk journalføring uten
// mennesker involvert, skal enhet settes til "9999".
val AUTOMATISK_JOURNALFOERING_ENHET = "9999"

@KtorExperimentalAPI
class DokArkivClient(
    private val url: String,
    private val oidcClient: AccessTokenProvider,
    private val httpClient: HttpClient
) {
    private val log: org.slf4j.Logger = LoggerFactory.getLogger("DokArkivClient")

    /**
     * Ved suksessfull ferdigstilling: 200 OK.
     *
     * Ved feil:
     *
     * 400 Bad Request. Kan ikke ferdigstille. Enten lar ikke journalposten seg ferdigstille eller så er input ugyldig.
     * 401 Unauthorized. Konsument kaller tjenesten med ugyldig OIDC-token.
     * 403 Forbidden. Konsument har ikke tilgang til å ferdigstille journalpost.
     * 500 Internal Server Error. Dersom en uventet feil oppstår i dokarkiv.
     */
    suspend private fun ferdigstillJournalpost(
        journalpostId: String,
        msgId: String,
        ferdigstillRequest: FerdigstillRequest
    ): String = retry("ferdigstill_journalpost") {
        try {
            return@retry httpClient.patch<String>("$url/journalpost/$journalpostId/ferdigstill") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("Authorization", "Bearer ${oidcClient.getToken()}")
                header("Nav-Callid", msgId)
                body = ferdigstillRequest
            }.also { log.info("ferdigstilling av journalpost ok for journalpostid {}, msgId {}, {}", journalpostId, msgId ) }
        } catch (e: Exception) {
            if (e is ClientRequestException) {
                when (e.response.status) {
                    HttpStatusCode.NotFound -> {
                        log.error("Journalposten finnes ikke for journalpostid {}, msgId {}, {}", journalpostId, msgId )
                        throw RuntimeException("Ferdigstilling: Journalposten finnes ikke for journalpostid $journalpostId msgid $msgId")
                    }
                    else -> {
                        log.error("Fikk http status {} for journalpostid {}, msgId {}, {}", e.response.status, journalpostId, msgId )
                        throw RuntimeException("Fikk feilmelding ved ferdigstilling av journalpostid $journalpostId msgid $msgId")
                    }
                }
            }
            log.error("Dokarkiv svarte med feilmelding ved ferdigstilling av journalpost for msgId {}, {}", msgId )
            throw IOException("Dokarkiv svarte med feilmelding ved ferdigstilling av journalpost for $journalpostId msgid $msgId")
        }
    }

    suspend fun ferdigstillJournalpost(
        journalpostId: String,
        msgId: String,
    ): String {
        return ferdigstillJournalpost(journalpostId, msgId, FerdigstillRequest(AUTOMATISK_JOURNALFOERING_ENHET))
    }

    /**
     *
     *
     * https://confluence.adeo.no/display/BOA/oppdaterJournalpost
     */
    suspend private fun oppdaterJournalpost(
        journalpostId: String,
        oppdaterJournalpostRequest: OppdaterJournalpostRequest,
        msgId: String
    ) = retry("oppdater_journalpost") {
        try {
            httpClient.put<HttpResponse>("$url/journalpost/$journalpostId") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("Authorization", "Bearer ${oidcClient.getToken()}")
                header("Nav-Callid", msgId)
                body = oppdaterJournalpostRequest
            }.also { log.info("Oppdatering av journalpost ok for journalpostid {}, msgId {}, {}", journalpostId, msgId ) }
        } catch (e: Exception) {
            if (e is ClientRequestException) {
                when (e.response.status) {
                    HttpStatusCode.NotFound -> {
                        log.error("Oppdatering: Journalposten finnes ikke for journalpostid {}, msgId {}, {}", journalpostId, msgId )
                        throw RuntimeException("Oppdatering: Journalposten finnes ikke for journalpostid $journalpostId msgid $msgId")
                    }
                    else -> {
                        log.error("Fikk http status {} ved oppdatering av journalpostid {}, msgId {}, {}", e.response.status, journalpostId, msgId )
                        throw RuntimeException("Fikk feilmelding ved oppdatering av journalpostid $journalpostId msgid $msgId")
                    }
                }
            }
            log.error("Dokarkiv svarte med feilmelding ved oppdatering av journalpost for msgId {}, {}", msgId )
            throw IOException("Dokarkiv svarte med feilmelding ved oppdatering av journalpost for $journalpostId msgid $msgId")
        }
    }

    suspend fun oppdaterJournalpost(
        journalpostId: String,
        fnr: String,
        isFnr: Boolean,
        gsakId: String,
        msgId: String
    ): HttpResponse {
        val req = OppdaterJournalpostRequest(
            bruker = Bruker(fnr, if (isFnr) { "FNR" } else { "ORGNR"} ),
            avsenderMottaker = AvsenderMottaker(fnr, "Arbeidsgiver"),
            sak = Sak("ARKIVSAK", "GSAK", gsakId )
        )
        return oppdaterJournalpost(journalpostId, req, msgId )
    }






}
