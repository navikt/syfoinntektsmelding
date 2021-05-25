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
import java.net.ConnectException

class AktorConsumer(
    private val tokenConsumer: TokenConsumer,
    private val username: String,
    private val endpointUrl: String,
    private val httpClient: HttpClient
) {
    private val log = LoggerFactory.getLogger(AktorConsumer::class.java)

    @Throws(AktørException::class)
    fun getAktorId(fnr: String): String {
        return getIdent(fnr, "AktoerId")
    }

    @Throws(AktørException::class)
    private fun getIdent(sokeIdent: String, identgruppe: String): String {
        var aktor: Aktor? = null

        runBlocking {
            val urlString = "$endpointUrl/identer?gjeldende=true&identgruppe=$identgruppe"
            try {
                aktor = httpClient.get<AktorResponse> {
                    url(urlString)
                    header("Authorization", "Bearer ${tokenConsumer.token}")
                    header("Nav-Call-Id", "${getFromMDC(MDC_CALL_ID)}")
                    header("Nav-Consumer-Id", "$username")
                    header("Nav-Personidenter", "$sokeIdent")
                }[sokeIdent]

            } catch (cause: ClientRequestException) {
                val status = cause.response?.status?.value
                log.error("Kall mot aktørregister på $endpointUrl feiler med HTTP-$status")
                throw AktørKallResponseException(status, null)
            } catch (cause: ConnectException) {
                log.error("Kall til $urlString gir ${cause.message}")
                throw AktørKallResponseException(999, cause)
            }
            if (aktor?.identer == null) {
                log.error("Fant ikke aktøren: ${aktor?.feilmelding}")
                throw FantIkkeAktørException(null);
            }
        }
        return aktor?.identer?.firstOrNull()?.ident.toString()
    }
}
