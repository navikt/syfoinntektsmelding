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
import no.nav.syfo.web.api.syfoinntektsmelding
import org.koin.ktor.ext.get

fun Application.inntektsmeldingModule(config: ApplicationConfig = environment.config) {
    install(Authentication) {
        tokenValidationSupport(config = config)
    }
    install(ContentNegotiation) {
        val commonObjectMapper = get<ObjectMapper>()
        register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
    }
    routing {
        route("/api/v1") {
            authenticate {
                syfoinntektsmelding(get(), get(), get())
            }
        }
    }
}
