package no.nav.syfo.rest

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.consumer.rest.TokenConsumer
import no.nav.syfo.consumer.rest.aktor.Aktor
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.rest.aktor.AktorResponse
import no.nav.syfo.consumer.rest.aktor.Ident
import no.nav.syfo.util.MDCOperations
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import slowtests.SystemTestBase
import java.util.*

class AktorConsumerTest {

    private val tokenConsumer = mockk<TokenConsumer>(relaxed = true)

    private var aktorConsumer = mockk<AktorConsumer>(relaxed = true)

    private var httpClient = mockk<HttpClient>(relaxed = true)

    @Before
    fun setup() {
        aktorConsumer = AktorConsumer(tokenConsumer!!, "username", "https://aktor.nav.no", httpClient)
        every{tokenConsumer.token} returns("token")
    }

    @Test
    fun finnerAktorId() {
        val response = AktorResponse()
        response["fnr"] = Aktor(
            Arrays.asList(
                Ident("aktorId", "AktoerId", true)
            ),
            null
        )

        coEvery{ hint(AktorResponse::class)
            httpClient.get<AktorResponse>(
                url {
                    protocol = URLProtocol.HTTPS
                    host = any()
                }
            ) {
                header("Authorization", any())
                header("Nav-Call-Id", any())
                header("Nav-Consumer-Id", any())
                header("Nav-Personidenter", any())
            }
        } returns response

        val aktorId = aktorConsumer.getAktorId("fnr")
        Assertions.assertThat(aktorId).isEqualTo("aktorId")
    }

//    @Test(expected = RuntimeException::class)
//    fun finnerIkkeIdent() {
//        val response = AktorResponse()
//        response["fnr"] = Aktor(null, "Fant ikke aktør")
//        Mockito.`when`(
//            restTemplate!!.exchange(
//                ArgumentMatchers.anyString(),
//                ArgumentMatchers.any(HttpMethod::class.java),
//                ArgumentMatchers.any(HttpEntity::class.java),
//                ArgumentMatchers.any<Any>() as Class<AktorResponse>
//            )
//        ).thenReturn(ResponseEntity(response, HttpStatus.OK))
//        aktorConsumer!!.getAktorId("fnr")
//    }

//    @Test(expected = RuntimeException::class)
//    fun manglendeFnrIResponsGirFeilmelding() {
//        val response = AktorResponse()
//        response["etAnnetFnr"] = Aktor(
//            Arrays.asList(
//                Ident("aktorId", "AktoerId", true)
//            ),
//            null
//        )
//        Mockito.`when`(
//            restTemplate!!.exchange(
//                ArgumentMatchers.anyString(),
//                ArgumentMatchers.any(HttpMethod::class.java),
//                ArgumentMatchers.any(HttpEntity::class.java),
//                ArgumentMatchers.any<Any>() as Class<AktorResponse>
//            )
//        ).thenReturn(ResponseEntity(response, HttpStatus.OK))
//        aktorConsumer!!.getAktorId("fnr")
//    }
//
//    @Test(expected = RuntimeException::class)
//    fun manglendeIdentGirFeilmelding() {
//        val response = AktorResponse()
//        response["fnr"] = Aktor(null, null)
//        Mockito.`when`(
//            restTemplate!!.exchange(
//                ArgumentMatchers.anyString(),
//                ArgumentMatchers.any(HttpMethod::class.java),
//                ArgumentMatchers.any(HttpEntity::class.java),
//                ArgumentMatchers.any<Any>() as Class<AktorResponse>
//            )
//        ).thenReturn(ResponseEntity(response, HttpStatus.OK))
//        aktorConsumer!!.getAktorId("fnr")
//    }
}
