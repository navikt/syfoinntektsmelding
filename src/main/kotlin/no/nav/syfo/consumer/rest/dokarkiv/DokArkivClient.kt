package no.nav.syfo.consumer.rest.dokarkiv

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

@KtorExperimentalAPI
class DokArkivClient(
    private val url: String,
    private val oidcClient: AccessTokenProvider,
    private val httpClient: HttpClient
) {
    private val log: org.slf4j.Logger = LoggerFactory.getLogger("DokArkivClient")

    private suspend fun ferdigstillJournalpost(
        journalpostId: String,
        msgId: String,
    ): String = retry("ferdigstill_journalpost") {
        try {
            return@retry httpClient.patch<String>("$url/$journalpostId/ferdigstill") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("Authorization", "Bearer ${oidcClient.getToken()}")
                header("Nav-Callid", msgId)
                body = FerdigstillJournal("9999")
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

    suspend fun oppdaterJournalpost(
        journalpostId: String,
        fnr: String,
        behandler: Behandler,
        msgId: String
    ) = retry("oppdater_journalpost") {
        try {
            httpClient.put<HttpResponse>("$url/$journalpostId") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("Authorization", "Bearer ${oidcClient.getToken()}")
                header("Nav-Callid", msgId)
                body = OppdaterJournalpost(
                    avsenderMottaker = AvsenderMottaker(
                        id = hprnummerMedRiktigLengde(behandler.hpr!!),
                        navn = finnNavn(behandler)
                    ),
                    bruker = Bruker(id = fnr),
                    sak = Sak()
                )
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

    private fun hprnummerMedRiktigLengde(hprnummer: String): String {
        if (hprnummer.length < 9) {
            return hprnummer.padStart(9, '0')
        }
        return hprnummer
    }

    private fun finnNavn(behandler: Behandler): String {
        return "${behandler.fornavn} ${behandler.etternavn}"
    }

    data class Behandler(
        val fornavn: String,
        val etternavn: String,
        val hpr: String,
    )

    data class FerdigstillJournal(
        val journalfoerendeEnhet: String
    )

    data class OppdaterJournalpost(
        val tema: String = "SYM",
        val avsenderMottaker: AvsenderMottaker,
        val bruker: Bruker,
        val sak: Sak
    )

    data class AvsenderMottaker(
        val id: String,
        val idType: String = "HPRNR",
        val navn: String
    )

    data class Bruker(
        val id: String,
        val idType: String = "FNR"
    )

    data class Sak(
        val sakstype: String = "GENERELL_SAK"
    )
}
