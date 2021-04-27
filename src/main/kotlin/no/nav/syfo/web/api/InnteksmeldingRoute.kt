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
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import no.nav.syfo.prosesser.FjernInnteksmeldingByBehandletProcessor
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource
import no.nav.syfo.web.api.Resultat

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
            datasource.connection.use { connection ->
                bakgunnsjobbService.opprettJobb<FjernInnteksmeldingByBehandletProcessor>(
                    kjoeretid = LocalDate.now().plusDays(1).atStartOfDay().plusHours(4),
                    maksAntallForsoek = 10,
                    data = om.writeValueAsString(FjernInnteksmeldingByBehandletProcessor.JobbData(UUID.randomUUID())),
                    connection = connection
                )
                bakgunnsjobbService.opprettJobb<FinnAlleUtgaandeOppgaverProcessor>(
                    kjoeretid = LocalDate.now().plusDays(1).atStartOfDay(),
                    maksAntallForsoek = 10,
                    data = om.writeValueAsString(FinnAlleUtgaandeOppgaverProcessor.JobbData(UUID.randomUUID())),
                    connection = connection
                )
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

