package no.nav.syfo.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.syfo.behandling.InntektsmeldingBehandler

class InnteksmeldingRoute {
    @KtorExperimentalAPI
    fun Route.syfoinntektsmelding(
        inntektsmeldingBehandler : InntektsmeldingBehandler
    ) {
        route("/api/v1/inntektsmelding") {
            post("behandleJournal"){
                    val request = call.receive<JournalInntektsmeldingRequest>()
                    val uuid = inntektsmeldingBehandler.behandle( request.arkivId, request.arkivReferanse )
                    uuid?.let{
                        call.respond(HttpStatusCode.Created,Resultat(uuid))
                    }
                    call.respond(HttpStatusCode.NoContent)
    }
            post("behandle") {
                    val request = call.receive<InntektsmeldingRequest>()
                    val uuid = inntektsmeldingBehandler.behandle(
                        request.inntektsmelding.journalpostId,
                        request.inntektsmelding.arkivRefereranse,
                        request.inntektsmelding
                    )
                    uuid?.let {
                        call.respond(HttpStatusCode.Created,Resultat(uuid))
                    }
                    call.respond(HttpStatusCode.NoContent)
            }
            }
}
}
