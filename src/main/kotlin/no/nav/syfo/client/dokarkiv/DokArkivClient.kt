package no.nav.syfo.client.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.util.navCallId
import java.io.IOException

// NAV-enheten som personen som utfører journalføring jobber for. Ved automatisk journalføring uten
// mennesker involvert, skal enhet settes til "9999".
const val AUTOMATISK_JOURNALFOERING_ENHET = "9999"

class DokArkivClient(
    private val url: String,
    private val httpClient: HttpClient,
    private val getAccessToken: () -> String,
) {
    private val logger = this.logger()
    private val sikkerlogger = sikkerLogger()

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
        msgId: String,
    ): String {
        try {
            val httpResponse =
                httpClient.patch("$url/journalpost/$journalpostId/ferdigstill") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    header("Authorization", "Bearer ${getAccessToken()}")
                    header("Nav-Callid", msgId)
                    setBody(ferdigstillRequest)
                }
            httpResponse.also { logger.info("Ferdigstilte journalpost {}", journalpostId) }
            return httpResponse.call.response.body()
        } catch (e: Exception) {
            if (e is ClientRequestException) {
                when (e.response.status) {
                    HttpStatusCode.NotFound -> {
                        "Journalposten finnes ikke for journalpostid $journalpostId".also {
                            logger.error(it)
                            sikkerlogger.error(it, e)
                            throw RuntimeException("Ferdigstilling: $it", e)
                        }
                    }

                    else -> {
                        "Fikk http status ${e.response.status} for journalpostid $journalpostId".also {
                            logger.error(it)
                            sikkerlogger.error(it, e)
                            throw RuntimeException("Ferdigstilling: Fikk feilmelding for journalpostid $journalpostId", e)
                        }
                    }
                }
            } else {
                "Ferdigstilling: Dokarkiv svarte med feilmelding for journalpost $journalpostId".also {
                    logger.error(it)
                    sikkerlogger.error(it, e)
                }
            }
            throw IOException("Ferdigstilling: Dokarkiv svarte med feilmelding for journalpost $journalpostId", e)
        }
    }

    suspend fun ferdigstillJournalpost(
        journalpostId: String,
        msgId: String,
    ): String = ferdigstillJournalpost(journalpostId, FerdigstillRequest(AUTOMATISK_JOURNALFOERING_ENHET), msgId)

    /**
     *
     *
     * https://confluence.adeo.no/display/BOA/oppdaterJournalpost
     */

    suspend fun oppdaterJournalpost(
        journalpostId: String,
        oppdaterJournalpostRequest: OppdaterJournalpostRequest,
        callId: String,
    ): HttpStatusCode {
        val idFragment = "journalpostId=[$journalpostId] callId=[$callId]"

        return runCatching {
            httpClient.put("$url/journalpost/$journalpostId") {
                contentType(ContentType.Application.Json)
                bearerAuth(getAccessToken())
                navCallId(callId)
                setBody(oppdaterJournalpostRequest)
            }
        }.onFailure { e ->
            if (e is ResponseException) {
                when (e.response.status) {
                    HttpStatusCode.NotFound -> {
                        "Journalposten finnes ikke for journalpostid $journalpostId".also {
                            logger.error(it)
                            sikkerlogger.error(it, e)
                            throw RuntimeException("Oppdatering: $it", e)
                        }
                    }

                    else -> {
                        "Feil ved oppdatering av journalpost $journalpostId".also {
                            logger.error(it)
                            sikkerlogger.error(it, e)
                            throw RuntimeException("Oppdatering: $it", e)
                        }
                    }
                }
            }
            throw e
        }.onSuccess {
            logger.info("Oppdatering av journalpost OK. $idFragment")
        }.getOrThrow()
            .status
    }

    suspend fun feilregistrerJournalpost(
        journalpostId: String,
        msgId: String,
    ) {
        try {
            httpClient
                .patch("$url/journalpost/$journalpostId/feilregistrer/feilregistrerSakstilknytning") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    header("Authorization", "Bearer ${getAccessToken()}")
                    header("Nav-Callid", msgId)
                }.also { logger.info("Feilregistrerte journalpost {}", journalpostId) }
        } catch (e: Exception) {
            if (e is ClientRequestException) {
                when (e.response.status) {
                    HttpStatusCode.NotFound -> {
                        "Klarte ikke feilregistrere journalpost $journalpostId".also {
                            logger.error(it)
                            sikkerlogger.error(it, e)
                            RuntimeException("Feilregistrering: Journalposten finnes ikke for journalpostid $journalpostId", e)
                        }
                    }

                    else -> {
                        "Fikk http status ${e.response.status} ved feilregistrering av journalpost $journalpostId".also {
                            logger.error(it)
                            sikkerlogger.error(it, e)
                            throw RuntimeException("Fikk feilmelding ved feilregistrering av journalpostid $journalpostId", e)
                        }
                    }
                }
            }
            "Dokarkiv svarte med feilmelding ved feilregistrering av journalpost $journalpostId".also {
                logger.error(it)
                sikkerlogger.error(it, e)
                throw RuntimeException(it, e)
            }
        }
    }
}
