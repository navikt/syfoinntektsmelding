package no.nav.syfo.consumer.rest.aktor

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandling.AktørException
import no.nav.syfo.behandling.AktørKallResponseException
import no.nav.syfo.behandling.FantIkkeAktørException
import no.nav.syfo.consumer.rest.TokenConsumer
import no.nav.syfo.util.MDCOperations.Companion.MDC_CALL_ID
import no.nav.syfo.util.MDCOperations.Companion.getFromMDC
import org.slf4j.LoggerFactory

class AktorConsumer(
    private val tokenConsumer: TokenConsumer,
    private val username: String,
    private val url: String,
    private val httpClient: HttpClient
) {
    private val log = LoggerFactory.getLogger(AktorConsumer::class.java)

    @Throws(AktørException::class)
    fun getAktorId(fnr: String): String {
        return getIdent(fnr, "AktoerId")
    }

    @Throws(AktørException::class)
    private fun getIdent(sokeIdent: String, identgruppe: String): String {
        var aktor : Aktor? = null
        val params = ParametersBuilder()
        params.append("gjeldende", "true")
        params.append("identgruppe", identgruppe)
        val genUrl = URLBuilder(URLProtocol.HTTPS, "$url/identer", parameters = params).toString()
        runBlocking {
            try {
                aktor = httpClient.get<AktorResponse>(
                    url {
                        protocol = URLProtocol.HTTPS
                        host = genUrl
                    }
                ) {
                    header("Authorization", "Bearer ${tokenConsumer.token}")
                    header("Nav-Call-Id", "${getFromMDC(MDC_CALL_ID)}")
                    header("Nav-Consumer-Id", "$username")
                    header("Nav-Personidenter", "$sokeIdent")
                }[sokeIdent]
            } catch (cause: Throwable) {
                val status = (cause as ClientRequestException).response?.status?.value
                log.error("Kall mot aktørregister feiler med HTTP-$status")
                throw AktørKallResponseException(status, null)
            }
            if ( aktor?.identer == null) {
                log.error("Fant ikke aktøren: ${aktor?.feilmelding}")
                throw FantIkkeAktørException(null);
            }
        }
        return aktor?.identer?.firstOrNull()?.ident.toString()
    }
}
