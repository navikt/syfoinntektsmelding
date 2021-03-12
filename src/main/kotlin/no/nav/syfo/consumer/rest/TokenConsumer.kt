package no.nav.syfo.consumer.rest

import lombok.extern.slf4j.Slf4j
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import no.nav.syfo.behandling.TokenException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import java.util.*

@Slf4j
@Component
class TokenConsumer(
    private val basicAuthRestTemplate: RestTemplate,
    @param:Value("\${security-token-service-token.url}") private val url: String
) {
    val token: String
        get() {
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
            val uriString = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("grant_type", "client_credentials")
                .queryParam("scope", "openid")
                .toUriString()
            val result =
                basicAuthRestTemplate.exchange(uriString, HttpMethod.GET, HttpEntity<Any>(headers), Token::class.java)
            if (result.statusCode != HttpStatus.OK) {
                throw TokenException(result.statusCode.value(), null)
            }
            return Objects.requireNonNull(result.body).access_token
        }
}
