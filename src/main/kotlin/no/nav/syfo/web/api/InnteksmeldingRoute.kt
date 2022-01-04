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
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.syfo.behandling.InntektsmeldingBehandler
import javax.sql.DataSource

fun Route.syfoinntektsmelding(
    inntektsmeldingBehandler: InntektsmeldingBehandler,
    bakgunnsjobbService: BakgrunnsjobbService,
    datasource: DataSource,
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
                request.inntektsmelding.arkivRefereranse,
                request.inntektsmelding
            )
            uuid?.let {
                call.respond(HttpStatusCode.Created, Resultat(uuid))
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
