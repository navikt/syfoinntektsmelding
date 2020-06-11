package no.nav.syfo.consumer

import log
import no.nav.syfo.behandling.SakFeilException
import no.nav.syfo.behandling.SakResponseException
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate

@Component
class SakConsumer(
    val restTemplate: RestTemplate,
    val azureAdTokenConsumer: AzureAdTokenConsumer,
    @Value("\${aad.syfogsak.clientid.username}") val syfogsakClientId: String
) {

    val log = log()

    fun finnSisteSak(aktorId: String, fom: LocalDate?, tom: LocalDate?): String? {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("Authorization", "Bearer " + azureAdTokenConsumer.getAccessToken(syfogsakClientId))

        val uriBuilder = UriComponentsBuilder.fromHttpUrl("http://syfogsak.default.svc.nais.local/$aktorId/sisteSak")

        if (fom != null && tom != null) {
            uriBuilder
                .queryParam("fom", fom)
                .queryParam("tom", tom)
        }

        val result: ResponseEntity<SisteSakRespons>
        result =
            restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, HttpEntity<Any>(headers), SisteSakRespons::class.java)

        if (result.statusCode != HttpStatus.OK) {
            val message = "Kall mot syfonarmesteleder feiler med HTTP-" + result.statusCode
            log.error(message)
            throw SakResponseException(aktorId, result.statusCode.value(), null)
        }

        try {
            return result.body?.sisteSak

        } catch (exception: Exception) {
            val message = "Uventet feil ved henting av nærmeste leder"
            log.error(message)
            throw SakFeilException(aktorId, exception)
        }

    }
}

data class SisteSakRespons(
    val sisteSak: String?
)
