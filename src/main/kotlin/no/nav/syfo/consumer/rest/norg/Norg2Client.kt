package no.nav.syfo.consumer.rest.norg

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.integrasjoner.AccessTokenProvider
import java.time.LocalDate

/**
 * Klient som henter alle arbeidsfordelinger
 *
 * https://confluence.adeo.no/pages/viewpage.action?pageId=178072651
 *
 * https://norg2.dev.adeo.no/norg2/swagger-ui.html#/arbeidsfordeling/findArbeidsfordelingByCriteriaUsingPOST
 *
 */
open class Norg2Client (
    private val url: String, private val stsClient: AccessTokenProvider, private val httpClient: HttpClient
)  {

    open suspend fun hentAlleArbeidsfordelinger(request: ArbeidsfordelingRequest, callId: String?): List<ArbeidsfordelingResponse> {
        val stsToken = stsClient.getToken()
        return runBlocking {
            httpClient.post<List<ArbeidsfordelingResponse>>(url) {
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                header("Authorization", "Bearer $stsToken")
                header("X-Correlation-ID", callId)
                body = request
            }
        }
    }
}

data class ArbeidsfordelingRequest(
    var behandlingstema	: String? = null,
    var behandlingstype	: String? = null,
    var diskresjonskode	: String? = null,
    var enhetNummer	: String? = null,
    var geografiskOmraade	: String? = null,
    var oppgavetype	: String? = null,
    var skjermet:	Boolean? = null,
    var tema	: String? = null,
    var temagruppe	: String? = null
)

data class ArbeidsfordelingResponse(
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val diskresjonskode: String? = null,
    val enhetId: Int,
    val enhetNavn: String,
    val enhetNr: String,
    val geografiskOmraade: String? = null,
    val gyldigFra: LocalDate? = null,
    val gyldigTil: LocalDate? = null,
    val id: Int,
    val oppgavetype: String,
    val skalTilLokalkontor: Boolean,
    val tema: String,
    val temagruppe: String,
)
