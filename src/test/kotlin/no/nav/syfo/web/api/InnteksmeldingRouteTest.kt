package no.nav.syfo.web.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.service.InntektsmeldingService
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class InnteksmeldingRouteTest {
    val inntektsmeldingService = mockk<InntektsmeldingService>(relaxed = true)

    @Test
    fun finnInntektsmeldingerReturnsOK() {
        every { inntektsmeldingService.finnInntektsmeldinger(any()) }
            .returns(listOf(mockk<Inntektsmelding>(relaxed = true)))
        testApplication {
            application {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                    }
                }
                routing {
                    finnInntektsmeldinger(inntektsmeldingService)
                }
            }
            val request =
                FinnInntektsmeldingerRequest(
                    fnr = "07025032327",
                    fom = LocalDate.now().minusDays(10),
                    tom = LocalDate.now(),
                )
            val response =
                client.post("/soek") {
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(
                        jacksonObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(request),
                    )
                }
            assertEquals(HttpStatusCode.OK, response.status)
            verify { inntektsmeldingService.finnInntektsmeldinger(any()) }
        }
    }

    @Test
    fun finnInntektsmeldingerBadrequestUgyldigFnr() {
        testApplication {
            application {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                    }
                }
                routing {
                    finnInntektsmeldinger(inntektsmeldingService)
                }
            }
            val request =
                FinnInntektsmeldingerRequest(
                    fnr = "1111111111",
                    fom = LocalDate.now().minusDays(10),
                    tom = LocalDate.now(),
                )
            val response =
                client.post("/soek") {
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(
                        jacksonObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(request),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }
}
