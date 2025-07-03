package no.nav.syfo.slowtests

import no.nav.syfo.SpinnApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.test.KoinTest

/**
 * Denne klassen kjører opp applikasjonen med Koin-profilen LOCAL
 * slik at man kan
 * 1) Kjøre tester mot HTTP-endepunktene slik de er i miljøene (Q+P)
 * 2) Kjøre tester mot systemet (bakgrunnsjobber feks) mens de er realistisk  konfigurert
 * 3) Kjøre ende til ende-tester (feks teste at en søknad send inn på HTTP-endepunktet havner i databasen riktig)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class SystemTestBase : KoinTest {
    companion object {
        const val TEST_SERVER_PORT = 8989
        var app: SpinnApplication? = null
    }

    @BeforeAll
    fun before() {
        if (app == null) {
            app = SpinnApplication(port = TEST_SERVER_PORT)
            app!!.start()
            Thread.sleep(200)
        }
    }

    @AfterAll
    fun after() {
    }
}
