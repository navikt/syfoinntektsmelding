package no.nav.syfo.client.norg

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.withCharset
import java.time.LocalDate

/**
 * Klient som henter alle arbeidsfordelinger
 *
 * Dokumentasjon
 * https://confluence.adeo.no/pages/viewpage.action?pageId=178072651
 *
 * Swagger
 * https://norg2.dev.adeo.no/norg2/swagger-ui.html#/arbeidsfordeling/findArbeidsfordelingByCriteriaUsingPOST
 *
 */
open class Norg2Client(
    private val url: String,
    private val httpClient: HttpClient,
) {
    /**
     * Oppslag av informasjon om ruting av arbeidsoppgaver til enheter.
     */
    open suspend fun hentAlleArbeidsfordelinger(
        request: ArbeidsfordelingRequest,
        callId: String?,
    ): List<ArbeidsfordelingResponse> {
        val httpResponse =
            httpClient.post(url) {
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                header("X-Correlation-ID", callId)
                header("Nav-Call-Id", callId)
                header("Nav-Consumer-Id", "spinosaurus")
                setBody(request)
            }

        return httpResponse.call.response.body()
    }
}

data class ArbeidsfordelingRequest(
    var behandlingstema: String? = null,
    var behandlingstype: String? = null,
    var diskresjonskode: String? = null,
    var enhetNummer: String? = null,
    var geografiskOmraade: String? = null,
    var oppgavetype: String? = null,
    var skjermet: Boolean? = null,
    var tema: String? = null,
    var temagruppe: String? = null,
)

data class ArbeidsfordelingResponse(
    val aktiveringsdato: LocalDate?,
    val antallRessurser: Int?,
    val enhetId: Int,
    val enhetNr: String? = null,
    val kanalstrategi: String?,
    val navn: String,
    val nedleggelsesdato: LocalDate?,
    val oppgavebehandler: Boolean?,
    val orgNivaa: String?,
    val orgNrTilKommunaltNavKontor: String?,
    val organisasjonsnummer: String?,
    val sosialeTjenester: String?,
    val status: String?,
    val type: String?,
    val underAvviklingDato: LocalDate?,
    val underEtableringDato: LocalDate?,
    val versjon: Int?,
)
