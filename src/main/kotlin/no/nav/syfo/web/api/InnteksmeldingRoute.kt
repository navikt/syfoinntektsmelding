@file:Suppress(
    "BlockingMethodInNonBlockingContext", "BlockingMethodInNonBlockingContext",
    "BlockingMethodInNonBlockingContext"
)

package no.nav.syfo.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.syfo.behandling.InntektsmeldingBehandler
import java.util.*
import javax.sql.DataSource

@KtorExperimentalAPI
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
