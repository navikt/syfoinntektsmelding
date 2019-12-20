package no.nav.syfo.consumer.azuread

import no.nav.syfo.api.AzureAdTokenConsumerException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.util.Objects.requireNonNull


@Component
class AzureAdTokenConsumer(private val restTemplateMedProxy: RestTemplate,
                           @param:Value("\${aadaccesstoken.url}") private val url: String,
                           @param:Value("\${aad.syfoinntektsmelding.clientid.username}") private val clientId: String,
                           @param:Value("\${aad.syfoinntektsmelding.clientid.password}") private val clientSecret: String) {

    fun getAccessToken(resource: String): String? {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val body = LinkedMultiValueMap<String, String>()
        body.add("client_id", clientId)
        body.add("resource", resource)
        body.add("grant_type", "client_credentials")
        body.add("client_secret", clientSecret)

        val uriString = UriComponentsBuilder.fromHttpUrl(url).toUriString()
        val result = restTemplateMedProxy.exchange(uriString, POST, HttpEntity<MultiValueMap<String, String>>(body, headers), AzureAdToken::class.java)
        if (result.statusCode != OK) {
            throw AzureAdTokenConsumerException(result.statusCode)
        }
        return requireNonNull<AzureAdToken>(result.body).access_token
    }
}

data class AzureAdToken(
        val access_token: String? = null,
        val token_type: String? = null,
        val expires_in: String? = null,
        val ext_expires_in: String? = null,
        val expires_on: String? = null,
        val not_before: String? = null,
        val resource: String? = null
)
