package no.nav.syfo.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.service.InntektsmeldingService
import no.nav.syfo.util.validerInntektsmelding

fun Route.spinosaurus(
    imRepo: InntektsmeldingRepository,
    om: ObjectMapper,
) {
    get("/{inntektsmeldingId}") {
        val inntektsmeldingId =
            call.parameters["inntektsmeldingId"] ?: throw IllegalArgumentException(
                "Forventet inntektsmeldingId som path parameter",
            )
        logger().info("Fikk request om å hente inntektsmelding med id: $inntektsmeldingId")

        try {
            val dto = imRepo.findByUuid(inntektsmeldingId)
            if (dto != null) {
                val inntektsmelding = toInntektsmelding(dto, om)

                val mappedInntektsmelding =
                    mapInntektsmeldingKontrakt(
                        inntektsmelding,
                        dto.aktorId,
                        validerInntektsmelding(inntektsmelding),
                        inntektsmelding.arkivRefereranse,
                        dto.uuid,
                    )

                logger().info("Henter inntektsmelding med arkivreferanse: ${inntektsmelding.arkivRefereranse}")

                call.respond(HttpStatusCode.OK, mappedInntektsmelding)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("code" to HttpStatusCode.NotFound.value, "message" to "Inntektsmelding ikke funnet"),
                )
                return@get
            }
        } catch (e: Exception) {
            logger().error("Feil ved henting av inntektsmelding id $inntektsmeldingId (se securelogs for mer detaljer)")
            sikkerLogger().error("Fikk feil for inntektsmelding id $inntektsmeldingId - ${e.message}", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("code" to HttpStatusCode.InternalServerError.value, "message" to "Feil ved henting av inntektsmelding"),
            )
        }
    }
}

fun Route.finnInntektsmeldinger(
    inntektsmeldingService: InntektsmeldingService,
) {
    post("/soek") {
        try {
            logger().info("Mottatt request for å finne inntektsmeldinger")
            val request = call.receive<FinnInntektsmeldingerRequest>()
            val inntektsmeldinger: List<no.nav.inntektsmeldingkontrakt.Inntektsmelding> = inntektsmeldingService.finnInntektsmeldinger(request)

            try {
                Fnr(request.fnr)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("code" to HttpStatusCode.BadRequest.value, "message" to "Ugyldig fødselsnummer"),
                )
                return@post
            }

            if (inntektsmeldinger.isEmpty()) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("code" to HttpStatusCode.NotFound.value, "message" to "Ingen inntektsmeldinger funnet"),
                )
                return@post
            }

            call.respond(HttpStatusCode.OK, inntektsmeldinger)
        } catch (e: Exception) {
            logger().error("Feil ved henting av inntektsmelding (se securelogs for mer detaljer)")
            sikkerLogger().error("Feil ved henting av inntektsmelding - ${e.message}", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("code" to HttpStatusCode.InternalServerError.value, "message" to "Feil ved henting av inntektsmeldinger"),
            )
        }
    }
}
