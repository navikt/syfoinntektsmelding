package no.nav.syfo.rest

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
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import java.util.*
import org.mockito.ArgumentMatchers.*

//@RunWith(MockitoJUnitRunner::class)
class NOT_USED_AktorConsumerTest {
    @Mock
    private val tokenConsumer: TokenConsumer? = null

    @Mock
    private lateinit var restTemplate: RestTemplate
    private var aktorConsumer: AktorConsumer? = null
    @Before
    fun setup() {
        aktorConsumer = AktorConsumer(tokenConsumer!!, "username", "https://aktor.nav.no", restTemplate!!)
        Mockito.`when`(tokenConsumer.token).thenReturn("token")
    }

    //@Ignore
    //@Test
    fun finnerAktorId() {
        val response = AktorResponse()
        response["fnr"] = Aktor(
            listOf(
                Ident("aktorId", "AktoerId", true)
            ),
            null
        )
        Mockito.`when`(
            restTemplate.exchange(
                eq(anyString()),
                any(HttpMethod::class.java),
                any(HttpEntity::class.java),
                AktorResponse::class.java
            )
        ).thenReturn(ResponseEntity(response, HttpStatus.OK))

        val aktorId = aktorConsumer?.getAktorId("fnr")
        Assertions.assertThat(aktorId).isEqualTo("aktorId")
    }
   /* @Ignore
    @Test(expected = RuntimeException::class)*/
    fun finnerIkkeIdent() {
        val response = AktorResponse()
        response["fnr"] = Aktor(null, "Fant ikke aktør")
        Mockito.`when`(
            restTemplate!!.exchange(
                "",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                AktorResponse::class.java
            )
        ).thenReturn(ResponseEntity(response, HttpStatus.OK))
        aktorConsumer!!.getAktorId("fnr")
    }

   /* @Ignore
    @Test(expected = RuntimeException::class)*/
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
                "",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                AktorResponse::class.java
            )
        ).thenReturn(ResponseEntity(response, HttpStatus.OK))
        aktorConsumer!!.getAktorId("fnr")
    }

   /* @Ignore
    @Test(expected = RuntimeException::class)*/
    fun manglendeIdentGirFeilmelding() {
        val response = AktorResponse()
        response["fnr"] = Aktor(null, null)
        Mockito.`when`(
            restTemplate!!.exchange(
                "",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                AktorResponse::class.java
            )
        ).thenReturn(ResponseEntity(response, HttpStatus.OK))
        aktorConsumer!!.getAktorId("fnr")
    }


}
