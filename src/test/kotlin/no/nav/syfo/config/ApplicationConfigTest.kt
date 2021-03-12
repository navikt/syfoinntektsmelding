package no.nav.syfo.config

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.web.WebAppConfiguration

@RunWith(SpringRunner::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
@WebAppConfiguration
class ApplicationConfigTest {
    companion object {
        init {
            System.setProperty("SECURITYTOKENSERVICE_URL", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_USERNAME", "joda")
            System.setProperty("SRVSYFOINNTEKTSMELDING_PASSWORD", "joda")
        }
    }

    @Test
    fun test() {
    }
}
