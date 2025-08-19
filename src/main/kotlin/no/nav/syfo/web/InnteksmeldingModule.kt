package no.nav.syfo.web

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.security.token.support.v3.tokenValidationSupport
import no.nav.syfo.util.customObjectMapper
import no.nav.syfo.util.getString
import no.nav.syfo.web.api.finnInntektsmeldinger
import no.nav.syfo.web.api.spinosaurus
import org.koin.ktor.ext.get

fun Application.inntektsmeldingModule(config: ApplicationConfig = environment.config) {
    val allowList = config.getString("aad_preauthorized_apps").let { customObjectMapper().readTree(it) }.map { it["clientId"].asText() }

    install(Authentication) {
        tokenValidationSupport(
            config = config,
            additionalValidation = {
                val claims = it.getClaims("hagproxy")
                val clientId = claims.getStringClaim("azp")
                clientId in allowList
            },
        )
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(customObjectMapper()))
        // Kan denne fjernes? JavaTimeModule er allerede registrert i customObjectMapper
        jackson {
            registerModule(JavaTimeModule())
        }
    }
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        route("/api/v1") {
            authenticate {
                route("/inntektsmelding") {
                    spinosaurus(get(), customObjectMapper())
                    finnInntektsmeldinger(get())
                }
            }
        }
    }
}
