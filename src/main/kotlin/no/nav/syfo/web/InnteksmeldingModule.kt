package no.nav.syfo.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.system.AppEnv
import no.nav.helse.arbeidsgiver.system.getEnvironment
import no.nav.helse.arbeidsgiver.system.getString
import no.nav.security.token.support.ktor.tokenValidationSupport
import no.nav.syfo.api.InnteksmeldingRoute
import no.nav.syfo.api.systemRoutes
import org.koin.ktor.ext.get
import org.slf4j.event.Level

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
fun Application.innteksmeldingModule(config: ApplicationConfig = environment.config) {
    install(CallLogging) {
        level = Level.INFO
    }

    install(Authentication) {
      //  tokenValidationSupport(config = config)
    }

    configureCORSAccess(config)
 //   configureExceptionHandling() TODO: Har dette bare med altinn å gjøre i fritak AGP, er dette noe vi trenger.

    install(ContentNegotiation) {
        val commonObjectMapper = get<ObjectMapper>()
        register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
    }

    routing {
        val apiBasePath = config.getString("ktor.application.basepath")
        route("$apiBasePath/api/v1") {
            systemRoutes()
            authenticate {
                InnteksmeldingRoute()
            }
        }
    }
}
private fun Application.configureCORSAccess(config: ApplicationConfig) {
    install(CORS)
    {
        method(HttpMethod.Options)
        method(HttpMethod.Post)
        method(HttpMethod.Get)

        when (config.getEnvironment()) {
            AppEnv.PROD -> host("arbeidsgiver.nav.no", schemes = listOf("https"))
            else -> anyHost()
        }

        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
}
