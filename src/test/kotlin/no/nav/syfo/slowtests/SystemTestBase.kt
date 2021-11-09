package no.nav.syfo.slowtests

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.setCookie
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.syfo.SpinnApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.test.KoinTest
import org.koin.test.inject

@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)

/**
 * Denne klassen kjører opp applikasjonen med Koin-profilen LOCAL
 * slik at man kan
 * 1) Kjøre tester mot HTTP-endepunktene slik de er i miljøene (Q+P)
 * 2) Kjøre tester mot systemet (bakgrunnsjobber feks) mens de er realistisk  konfigurert
 * 3) Kjøre ende til ende-tester (feks teste at en søknad send inn på HTTP-endepunktet havner i databasen riktig)
 */
open class SystemTestBase : KoinTest {

    protected val httpClient by inject<HttpClient>()

    companion object {
        const val testServerPort = 8989
        var app: SpinnApplication? = null
        init {
            System.setProperty("SECURITYTOKENSERVICE_URL", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", "joda")
        }
    }

    @BeforeAll
    fun before() {
        if (app == null) {
            app = SpinnApplication(port = testServerPort)
            app!!.start()
            Thread.sleep(200)
        }
    }

    @AfterAll
    fun after() {
    }

    /**
     * Hjelpefunksjon for å kalle HTTP-endepunktene med riktig port i testene
     */
    private fun HttpRequestBuilder.appUrl(relativePath: String) {
        url("http://localhost:$testServerPort$relativePath")
    }

    /**
     * Hjelpefunksjon for å hente ut gyldig JWT-token og legge det til som Auth header på en request
     */
    suspend fun HttpRequestBuilder.loggedInAs(subject: String) {
        val response = httpClient.get<HttpResponse> {
            appUrl("/local/cookie-please?subject=$subject")
            contentType(ContentType.Application.Json)
        }

        header("Authorization", "Bearer ${response.setCookie()[0].value}")
    }

    /**
     * Hjelpefunksjon for at JUnit5 skal kunne kjenne igjen tester som kaller har "suspend"-funksjoner
     */
    fun suspendableTest(block: suspend CoroutineScope.() -> Unit) {
        runBlocking {
            block()
            Unit
        }
    }
}
