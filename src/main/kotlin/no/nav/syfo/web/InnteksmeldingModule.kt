package no.nav.syfo.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.syfo.web.api.syfoinntektsmelding
import org.koin.ktor.ext.get
import org.slf4j.event.Level

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
fun Application.inntektsmeldingModule(config: ApplicationConfig = environment.config) {
    install(CallLogging) {
        level = Level.INFO
    }

    install(ContentNegotiation) {
        val commonObjectMapper = get<ObjectMapper>()
        register(ContentType.Application.Json, JacksonConverter(commonObjectMapper))
    }

    routing {
        route("/api/v1") {
            syfoinntektsmelding(get(), get(), get(), get())
        }
    }
}
