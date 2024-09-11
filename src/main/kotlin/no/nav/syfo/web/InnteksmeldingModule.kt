package no.nav.syfo.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.config.ApplicationConfig
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.route
import io.ktor.routing.routing
import no.nav.security.token.support.ktor.tokenValidationSupport
import no.nav.syfo.util.getString
import no.nav.syfo.web.api.syfoinntektsmelding
import org.koin.ktor.ext.get

fun Application.inntektsmeldingModule(config: ApplicationConfig = environment.config) {
    val commonObjectMapper = get<ObjectMapper>()
    val allowList = config.getString("aad_preauthorized_apps").let { commonObjectMapper.readTree(it) }.map { it["clientId"].asText() }
    install(Authentication) {
        tokenValidationSupport(
            config = config,
            additionalValidation = {
                val claims = it.getClaims("hagproxy")
                val roles = claims.getAsList("roles")
                val clientId = claims.getStringClaim("azp")
                clientId in allowList && roles.size == 1 && roles.contains("access_as_application")
            }
        )
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
    }
    routing {
        route("/api/v1") {
            authenticate {
                syfoinntektsmelding(get(), get())
            }
        }
    }
}
