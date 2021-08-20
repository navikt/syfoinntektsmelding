package no.nav.syfo.consumer.rest

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import no.nav.syfo.util.MDCOperations
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 *
 * Tjeneste som lar konsument "switche" status på en journalpost fra midlerdidig til endelig. Dersom journalposten ikke
 * er mulig å ferdigstille, for eksempel fordi den mangler påkrevde metadata, får konsument beskjed om hva som mangler.
 *
 * https://confluence.adeo.no/display/BOA/ferdigstillJournalpost
 *
 */
open class JournalpostClient (
    private val url: String,
    private val stsClient: AccessTokenProvider,
    private val httpClient: HttpClient
)  {
    private val logger: org.slf4j.Logger = LoggerFactory.getLogger("JournalpostClient")

    /**
     * Ved suksess
     * 200 OK.
     *
     * Feil:
     * 400 Bad Request. Kan ikke ferdigstille. Enten lar ikke journalposten seg ferdigstille eller så er input ugyldig.
     * 401 Unauthorized. Konsument kaller tjenesten med ugyldig OIDC-token.
     * 403 Forbidden. Konsument har ikke tilgang til å ferdigstille journalpost.
     * 500 Internal Server Error. Dersom en uventet feil oppstår i dokarkiv.
     *
     */
    open suspend fun ferdigstillJournalpost(journalpostId: String, journalfoerendeEnhet: String): HttpStatusCode {
        logger.info("Ferdigstiller journalpost $journalpostId for enhet $journalfoerendeEnhet")
        return httpClient.patch<HttpStatement>(url + "/journalpost/$journalpostId/ferdigstill") {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            header("Authorization", "Bearer ${stsClient.getToken()}")
            this.header("X-Correlation-ID", MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID))
            body = FerdigstillRequest(journalfoerendeEnhet = journalfoerendeEnhet)
        }.execute().status
    }
}

data class FerdigstillRequest (
    var journalfoerendeEnhet: String,
    var journalfortAvNavn: String? = null,
    var opprettetAvNavn: String? = null,
    var datoJournal: LocalDate? = null,
    var datoSendtPrint: LocalDate? = null,
)

