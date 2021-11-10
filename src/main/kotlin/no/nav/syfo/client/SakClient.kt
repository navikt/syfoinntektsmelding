package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.behandling.SakFeilException
import no.nav.syfo.behandling.SakResponseException
import no.nav.syfo.client.azuread.AzureAdTokenConsumer
import no.nav.syfo.mask.maskLast
import java.time.LocalDate

class SakConsumer(
    val httpClient: HttpClient,
    val azureAdTokenConsumer: AzureAdTokenConsumer,
    private val syfogsakClientId: String,
    private val hostUrl: String
) {
    val log = log()

    fun finnSisteSak(aktorId: String, fom: LocalDate?, tom: LocalDate?): String? {
        var result: String? = null
        val accessToken = azureAdTokenConsumer.getAccessToken(syfogsakClientId)
        runBlocking {
            val url = "$hostUrl/$aktorId/sisteSak" + if (fom != null && tom != null) "?fom=$fom&tom=$tom" else ""
            try {
                result = httpClient.get<SisteSakRespons>(url) {
                    header("Authorization", "Bearer $accessToken")
                }.sisteSak
            } catch (cause: Throwable) {
                when (cause) {
                    is ClientRequestException -> if (HttpStatusCode.OK.value != cause.response.status.value) {
                        log.error("Kall mot syfonarmesteleder mot $url feiler med HTTP-${cause.response.status.value}")
                        throw SakResponseException(aktorId, cause.response.status.value, null)
                    } else -> {
                        log.error("Uventet feil ved henting av nærmeste leder for ${aktorId.maskLast(3)} for perioden $fom til $tom")
                        throw SakFeilException(aktorId, Exception(cause.message))
                    }
                }
            }
        }
        return result
    }
}

data class SisteSakRespons(
    val sisteSak: String?
)
