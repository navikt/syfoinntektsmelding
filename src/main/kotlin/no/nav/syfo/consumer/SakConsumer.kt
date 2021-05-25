package no.nav.syfo.consumer

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import log
import no.nav.syfo.behandling.SakFeilException
import no.nav.syfo.behandling.SakResponseException
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import java.time.LocalDate

class SakConsumer(
    val httpClient : HttpClient,
    val azureAdTokenConsumer: AzureAdTokenConsumer,
    private val syfogsakClientId: String,
    private val hostUrl: String
) {
    val log = log()

    fun finnSisteSak(aktorId: String, fom: LocalDate?, tom: LocalDate?): String? {
        var result: String? = null
        runBlocking {
            try {
                result = httpClient.get<SisteSakRespons>(
                    url {
                        protocol = URLProtocol.HTTP
                        host = hostUrl
                        path("$aktorId","sisteSak")
                        parameters.append("fom", fom.toString())
                        parameters.append("tom", tom.toString())
                    }
                ) {
                    header("Authorization", "Bearer ${azureAdTokenConsumer.getAccessToken(syfogsakClientId)}")
                }.sisteSak
            } catch (cause: Throwable) {
                when(cause) {
                    is ClientRequestException -> if (HttpStatusCode.OK.value != cause.response.status.value) {
                        log.error("Kall mot syfonarmesteleder feiler med HTTP-${cause.response.status.value}")
                        throw SakResponseException(aktorId, cause.response.status.value, null)
                        }
                    else -> {
                        log.error("Uventet feil ved henting av nærmeste leder")
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
