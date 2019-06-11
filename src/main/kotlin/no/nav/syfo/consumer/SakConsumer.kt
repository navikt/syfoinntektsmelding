package no.nav.syfo.consumer

import log
import no.nav.syfo.consumer.azuread.AzureAdTokenConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
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

        val uriBuilder = UriComponentsBuilder.fromHttpUrl("http://syfogsak.default/$aktorId/sisteSak")

        if (fom != null && tom != null) {
            uriBuilder
                .queryParam("fom", fom)
                .queryParam("tom", tom)
        }

        val result: ResponseEntity<SisteSakRespons>
        result =
            restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, HttpEntity(null, headers), SisteSakRespons::class.java)

        if (result.statusCode != HttpStatus.OK) {
            val message = "Kall mot syfonarmesteleder feiler med HTTP-" + result.statusCode
            log.error(message)
            throw RuntimeException(message)
        }

        try {
            return result.body?.sisteSak

        } catch (exception: Exception) {
            val message = "Uventet feil ved henting av nærmeste leder"
            log.error(message)
            throw RuntimeException(message, exception)
        }

    }
}

data class SisteSakRespons(
    val sisteSak: String?
)
