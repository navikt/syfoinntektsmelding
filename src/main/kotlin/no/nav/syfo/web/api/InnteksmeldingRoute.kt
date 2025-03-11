package no.nav.syfo.web.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.mapping.mapInntektsmeldingKontrakt
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.util.validerInntektsmelding
import org.slf4j.LoggerFactory

fun Route.syfoinntektsmelding(
    imRepo: InntektsmeldingRepository,
    om: ObjectMapper,
) {
    val logger = this.logger()
    val sikkerlogger = LoggerFactory.getLogger("tjenestekall")

    route("/inntektsmelding") {
        get("/{inntektsmeldingId}") {
            val inntektsmeldingId =
                call.parameters["inntektsmeldingId"] ?: throw IllegalArgumentException(
                    "Forventet inntektsmeldingId som path parameter",
                )
            logger.info("Fikk request om å hente inntektsmelding med id: $inntektsmeldingId")

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

                    logger.info("Henter inntektsmelding med arkivreferanse: ${inntektsmelding.arkivRefereranse}")

                    call.respond(HttpStatusCode.OK, mappedInntektsmelding)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch (e: Exception) {
                logger.error("Feil ved henting av inntektsmelding id $inntektsmeldingId (se securelogs for mer detaljer)")
                sikkerlogger.error("Fikk veil for inntektsmelding id $inntektsmeldingId - ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
