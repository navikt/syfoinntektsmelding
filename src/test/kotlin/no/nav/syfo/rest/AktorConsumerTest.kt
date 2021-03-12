package no.nav.syfo.rest

import com.fasterxml.jackson.databind.util.ClassUtil.createInstance
import no.nav.syfo.consumer.rest.TokenConsumer
import no.nav.syfo.consumer.rest.aktor.Aktor
import no.nav.syfo.consumer.rest.aktor.AktorConsumer
import no.nav.syfo.consumer.rest.aktor.AktorResponse
import no.nav.syfo.consumer.rest.aktor.Ident
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

import java.util.*

@RunWith(MockitoJUnitRunner::class)
class AktorConsumerTest {
    @Mock
    private val tokenConsumer: TokenConsumer? = null

    @Mock
    private val restTemplate: RestTemplate? = null
    private var aktorConsumer: AktorConsumer? = null
    @Before
    fun setup() {
        aktorConsumer = AktorConsumer(tokenConsumer!!, "username", "https://aktor.nav.no", restTemplate!!)
        Mockito.`when`(tokenConsumer.token).thenReturn("token")
    }

    @Ignore
    @Test
    fun finnerAktorId() {
        val response = AktorResponse()
        response["fnr"] = Aktor(
            Arrays.asList(
                Ident("aktorId", "AktoerId", true)
            ),
            null
        )
        Mockito.`when`(
            restTemplate!!.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod::class.java),
                ArgumentMatchers.any(HttpEntity::class.java),
                ArgumentMatchers.any<Any>() as Class<AktorResponse>
            )
        )
            .thenReturn(ResponseEntity(response, HttpStatus.OK))
        val aktorId = aktorConsumer!!.getAktorId("fnr")
        Assertions.assertThat(aktorId).isEqualTo("aktorId")
    }

    @Ignore
    @Test(expected = RuntimeException::class)
    fun finnerIkkeIdent() {
        val response = AktorResponse()
        response["fnr"] = Aktor(null, "Fant ikke aktør")
        Mockito.`when`(
            restTemplate!!.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod::class.java),
                ArgumentMatchers.any(HttpEntity::class.java),
                ArgumentMatchers.any<Any>() as Class<AktorResponse>
            )
        ).thenReturn(ResponseEntity(response, HttpStatus.OK))
        aktorConsumer!!.getAktorId("fnr")
    }

    @Ignore
    @Test(expected = RuntimeException::class)
    fun manglendeFnrIResponsGirFeilmelding() {
        val response = AktorResponse()
        response["etAnnetFnr"] = Aktor(
            Arrays.asList(
                Ident("aktorId", "AktoerId", true)
            ),
            null
        )
        Mockito.`when`(
            restTemplate!!.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod::class.java),
                ArgumentMatchers.any(HttpEntity::class.java),
                ArgumentMatchers.any<Any>() as Class<AktorResponse>
            )
        ).thenReturn(ResponseEntity(response, HttpStatus.OK))
        aktorConsumer!!.getAktorId("fnr")
    }

    @Ignore
    @Test(expected = RuntimeException::class)
    fun manglendeIdentGirFeilmelding() {
        val response = AktorResponse()
        response["fnr"] = Aktor(null, null)
        Mockito.`when`(
            restTemplate!!.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod::class.java),
                ArgumentMatchers.any(HttpEntity::class.java),
                ArgumentMatchers.any<Any>() as Class<AktorResponse>
            )
        ).thenReturn(ResponseEntity(response, HttpStatus.OK))
        aktorConsumer!!.getAktorId("fnr")
    }


}
