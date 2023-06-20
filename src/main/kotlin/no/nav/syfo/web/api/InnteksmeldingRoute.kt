@file:Suppress(
    "BlockingMethodInNonBlockingContext", "BlockingMethodInNonBlockingContext",
    "BlockingMethodInNonBlockingContext"
)

package no.nav.syfo.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.syfo.behandling.InntektsmeldingBehandler
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.util.validerInntektsmelding

fun Route.syfoinntektsmelding(
    inntektsmeldingBehandler: InntektsmeldingBehandler,
    imRepo: InntektsmeldingRepository,
    om: ObjectMapper
) {
    route("/api/v1/inntektsmelding") {
        post("behandleJournal") {
            val request = call.receive<JournalInntektsmeldingRequest>()
            val uuid = inntektsmeldingBehandler.behandle(request.arkivId, request.arkivReferanse)
            uuid?.let {
                call.respond(HttpStatusCode.Created, Resultat(uuid))
            }
            call.respond(HttpStatusCode.NoContent)
        }
        post("behandle") {
            val request = call.receive<InntektsmeldingRequest>()
            val uuid = inntektsmeldingBehandler.behandle(
                request.inntektsmelding.journalpostId,
                request.inntektsmelding.arkivRefereranse
            )
            uuid?.let {
                call.respond(HttpStatusCode.Created, Resultat(uuid))
            }
            call.respond(HttpStatusCode.NoContent)
        }
        get("/{inntektsmeldingId}") {
            val inntektsmeldingId = call.parameters["inntektsmeldingId"] ?: throw IllegalArgumentException("Forventet inntektsmeldingId som path parameter")
            val dto = imRepo.findByUuid(inntektsmeldingId)
            if (dto != null) {
                val inntektsmelding = toInntektsmelding(dto, om)

                val mappedInntektsmelding = mapInntektsmeldingKontrakt(
                    inntektsmelding,
                    dto.aktorId,
                    validerInntektsmelding(inntektsmelding),
                    inntektsmelding.arkivRefereranse,
                    dto.uuid
                )

                call.respond(HttpStatusCode.OK, mappedInntektsmelding)
            } else call.respond(HttpStatusCode.NotFound)
        }
    }
}
