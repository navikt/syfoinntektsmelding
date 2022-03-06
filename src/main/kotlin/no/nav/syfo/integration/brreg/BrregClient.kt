package no.nav.syfo.integration.brreg

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get

interface BrregClient {
    suspend fun getVirksomhetsNavn(orgnr: String): String
}

data class UnderenheterNavnResponse(
    val navn: String
)

class MockBrregClient : BrregClient {
    override suspend fun getVirksomhetsNavn(orgnr: String): String {
        return "Stark Industries"
    }
}

class BrregClientImp(private val httpClient: HttpClient, private val brregUrl: String) :
    BrregClient {
    override suspend fun getVirksomhetsNavn(orgnr: String): String {
        return try {
            httpClient.get<UnderenheterNavnResponse>(brregUrl + orgnr).navn
        } catch (cause: ClientRequestException) {
            if (404 == cause.response.status.value) {
                "Ukjent arbeidsgiver"
            } else {
                throw cause
            }
        }
    }
}
