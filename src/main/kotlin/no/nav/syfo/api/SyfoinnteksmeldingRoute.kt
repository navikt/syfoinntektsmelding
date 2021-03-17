package no.nav.syfo.api

import io.swagger.annotations.ApiParam

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.Consumes
import javax.ws.rs.Produces
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.readAllParts
import io.ktor.http.content.streamProvider
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.*

class SyfoinnteksmeldingRoute {
    @KtorExperimentalAPI
    fun Route.syfoinntektsmelding(
        authorizer: AltinnAuthorizer,
        authRepo: AltinnOrganisationsRepository,
        refusjonskravService: RefusjonskravService
    ) {
        route("api/v1") {
        }
    }
}


//    @PostMapping("behandle")
//    @Produces(MediaType.TEXT_PLAIN_VALUE)
//    @Consumes(MediaType.APPLICATION_JSON_VALUE)
//    fun behandleInntektsmelding(
//        @ApiParam("Inntektsmelding")
//        @RequestBody request: InntektsmeldingRequest): ResponseEntity<Resultat> {
//        val uuid = inntektsmeldingBehandler.behandle( request.inntektsmelding.journalpostId, request.inntektsmelding.arkivRefereranse, request.inntektsmelding )
//        uuid?.let{
//            return ResponseEntity.status(HttpStatus.CREATED).body(Resultat(uuid))
//        }
//        return ResponseEntity.status(HttpServletResponse.SC_NO_CONTENT).build()
//    }
}
