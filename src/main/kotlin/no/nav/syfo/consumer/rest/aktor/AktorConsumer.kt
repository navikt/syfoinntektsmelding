package no.nav.syfo.consumer.rest.aktor

import lombok.extern.slf4j.Slf4j
import no.nav.syfo.behandling.*
import no.nav.syfo.consumer.rest.TokenConsumer
import no.nav.syfo.util.MDCOperations.Companion.MDC_CALL_ID
import no.nav.syfo.util.MDCOperations.Companion.getFromMDC
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Slf4j
@Component
class AktorConsumer(
    private val tokenConsumer: TokenConsumer,
    @param:Value("\${srvsyfoinntektsmelding.username}") private val username: String,
    @param:Value("\${aktoerregister.api.v1.url}") private val url: String,
    private val restTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(AktorConsumer::class.java)

    @Throws(AktørException::class)
    fun getAktorId(fnr: String): String {
        return getIdent(fnr, "AktoerId")
    }

    @Throws(AktørException::class)
    private fun getIdent(sokeIdent: String, identgruppe: String): String {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        headers["Authorization"] = "Bearer " + tokenConsumer.token
        headers["Nav-Call-Id"] = getFromMDC(MDC_CALL_ID)
        headers["Nav-Consumer-Id"] = username
        headers["Nav-Personidenter"] = sokeIdent

        val uriString = UriComponentsBuilder.fromHttpUrl("$url/identer")
            .queryParam("gjeldende", "true")
            .queryParam("identgruppe", identgruppe)
            .toUriString()

        return try {
            val result =
                restTemplate.exchange(uriString, HttpMethod.GET, HttpEntity<Any>(headers), AktorResponse::class.java)
            if (result.statusCode != HttpStatus.OK) {
                val message = "Kall mot aktørregister feiler med HTTP-" + result.statusCode
                log.error(message)
                throw AktørKallResponseException(result.statusCode.value(), null)
            }

            if (result == null) {
                log.error("Feil ved henting av aktorId")
                throw TomAktørListeException(null)
            }

            val aktor : Aktor? = result!!.body?.get(sokeIdent)
            if(aktor?.identer == null) {
                log.error("Fant ikke aktøren: " + aktor?.feilmelding);
                throw FantIkkeAktørException(null);
            }

            result?.body?.get(sokeIdent)?.identer?.firstOrNull()?.ident.toString()

        } catch (e: HttpClientErrorException) {
            log.error("Feil ved oppslag i aktørtjenesten")
            throw AktørOppslagException(e)
        }
    }
}
