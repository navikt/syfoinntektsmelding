package no.nav.syfo.client

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

interface BrregClient {
    fun getVirksomhetsNavn(orgnr: String): String
}

data class UnderenheterNavnResponse(
    val navn: String
)

class MockBrregClient : BrregClient {
    override fun getVirksomhetsNavn(orgnr: String): String {
        return "Stark Industries"
    }
}

class BrregClientImp(private val httpClient: HttpClient, private val brregUrl: String) :
    BrregClient {
    private val log = LoggerFactory.getLogger(OppgaveClient::class.java)

    override fun getVirksomhetsNavn(orgnr: String): String {
        return try {
            val (navn) = runBlocking {
                httpClient.get<UnderenheterNavnResponse>(brregUrl + orgnr)
            }
            log.info("Fant virksomheten")
            navn
        } catch (cause: ClientRequestException) {
            if (404 == cause.response.status.value) {
                log.error("Fant ikke virksomhet i brreg")
                "Arbeidsgiver"
            } else {
                log.error("Klarte ikke å hente virksomhet!", cause)
                throw cause
            }
        }
    }
}
