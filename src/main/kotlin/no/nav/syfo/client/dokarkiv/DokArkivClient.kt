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
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.helpers.retry
import java.io.IOException

// NAV-enheten som personen som utfører journalføring jobber for. Ved automatisk journalføring uten
// mennesker involvert, skal enhet settes til "9999".
val AUTOMATISK_JOURNALFOERING_ENHET = "9999"

class DokArkivClient(
    private val url: String,
    private val accessTokenProvider: AccessTokenProvider,
    private val httpClient: HttpClient
) {
    private val logger = this.logger()

    /**
     * Tjeneste som lar konsument "switche" status på en journalpost fra midlerdidig til endelig. Dersom journalposten
     * ikke er mulig å ferdigstille, for eksempel fordi den mangler påkrevde metadata, får konsument beskjed om hva
     * som mangler.
     *
     * https://confluence.adeo.no/display/BOA/ferdigstillJournalpost
     *
     * Ved suksessfull ferdigstilling: 200 OK.
     *
     * Ved feil:
     *
     * 400 Bad Request. Kan ikke ferdigstille. Enten lar ikke journalposten seg ferdigstille eller så er input ugyldig.
     * 401 Unauthorized. Konsument kaller tjenesten med ugyldig OIDC-token.
     * 403 Forbidden. Konsument har ikke tilgang til å ferdigstille journalpost.
     * 500 Internal Server Error. Dersom en uventet feil oppstår i dokarkiv.
     */
    suspend fun ferdigstillJournalpost(
        journalpostId: String,
        ferdigstillRequest: FerdigstillRequest,
        msgId: String
    ): String {
        try {
            return httpClient.patch<String>("$url/journalpost/$journalpostId/ferdigstill") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("Authorization", "Bearer ${accessTokenProvider.getToken()}")
                header("Nav-Callid", msgId)
                body = ferdigstillRequest
            }.also { logger.info("Ferdigstilte journalpost {}", journalpostId) }
        } catch (e: Exception) {
            if (e is ClientRequestException) {
                when (e.response.status) {
                    HttpStatusCode.NotFound -> {
                        logger.error("Journalposten finnes ikke for journalpostid $journalpostId", e)
                        throw RuntimeException("Ferdigstilling: Journalposten finnes ikke for journalpostid $journalpostId", e)
                    }
                    else -> {
                        logger.error("Fikk http status ${e.response.status} for journalpostid $journalpostId", e)
                        throw RuntimeException("Ferdigstilling: Fikk feilmelding for journalpostid $journalpostId", e)
                    }
                }
            } else {
                logger.error("Ferdigstilling: Dokarkiv svarte med feilmelding for journalpost $journalpostId", e)
            }
            throw IOException("Ferdigstilling: Dokarkiv svarte med feilmelding for journalpost $journalpostId", e)
        }
    }

    suspend fun ferdigstillJournalpost(
        journalpostId: String,
        msgId: String,
    ): String {
        return ferdigstillJournalpost(journalpostId, FerdigstillRequest(AUTOMATISK_JOURNALFOERING_ENHET), msgId)
    }

    /**
     *
     *
     * https://confluence.adeo.no/display/BOA/oppdaterJournalpost
     */
    suspend fun oppdaterJournalpost(
        journalpostId: String,
        oppdaterJournalpostRequest: OppdaterJournalpostRequest,
        msgId: String
    ) = retry("oppdater_journalpost") {
        try {
            httpClient.put<HttpResponse>("$url/journalpost/$journalpostId") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("Authorization", "Bearer ${accessTokenProvider.getToken()}")
                header("Nav-Callid", msgId)
                body = oppdaterJournalpostRequest
            }.also { logger.info("Oppdatering av journalpost ok for journalpostid {}, msgId {}", journalpostId, msgId) }
        } catch (e: Exception) {
            if (e is ClientRequestException) {
                when (e.response.status) {
                    HttpStatusCode.NotFound -> {
                        logger.error("Oppdatering: Journalposten finnes ikke for journalpostid {}, msgId {}", journalpostId, msgId)
                        throw RuntimeException("Oppdatering: Journalposten finnes ikke for journalpostid $journalpostId msgid $msgId")
                    }
                    else -> {
                        logger.error("Fikk http status {} ved oppdatering av journalpostid {}, msgId {}", e.response.status, journalpostId, msgId)
                        throw RuntimeException("Fikk feilmelding ved oppdatering av journalpostid $journalpostId msgid $msgId")
                    }
                }
            }
            logger.error("Dokarkiv svarte med feilmelding ved oppdatering av journalpost $journalpostId", e)
            throw IOException("Dokarkiv svarte med feilmelding ved oppdatering av journalpost for $journalpostId msgid $msgId")
        }
    }

    suspend fun feilregistrerJournalpost(journalpostId: String, msgId: String) {
        try {
            httpClient.patch<HttpResponse>("$url/journalpost/$journalpostId/feilregistrer/feilregistrerSakstilknytning") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("Authorization", "Bearer ${accessTokenProvider.getToken()}")
                header("Nav-Callid", msgId)
            }.also { logger.info("Feilregistrerte journalpost {}", journalpostId) }
        } catch (e: Exception) {
            if (e is ClientRequestException) {
                when (e.response.status) {
                    HttpStatusCode.NotFound -> {
                        logger.error("Klarte ikke feilregistrere journalpost $journalpostId", e)
                        throw RuntimeException("feilregistrering: Journalposten finnes ikke for journalpostid $journalpostId", e)
                    }
                    else -> {
                        logger.error("Fikk http status ${e.response.status} ved feilregistrering av journalpost $journalpostId", e)
                        throw RuntimeException("Fikk feilmelding ved feilregistrering av journalpostid $journalpostId", e)
                    }
                }
            }
            logger.error("Dokarkiv svarte med feilmelding ved feilregistrering av journalpost $journalpostId", e)
            throw IOException("Dokarkiv svarte med feilmelding ved feilregistrering av journalpost for $journalpostId", e)
        }
    }
}
