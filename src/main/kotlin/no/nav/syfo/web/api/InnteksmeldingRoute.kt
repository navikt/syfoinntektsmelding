@file:Suppress(
    "BlockingMethodInNonBlockingContext",
    "BlockingMethodInNonBlockingContext",
    "BlockingMethodInNonBlockingContext",
)

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

fun Route.syfoinntektsmelding(
    imRepo: InntektsmeldingRepository,
    om: ObjectMapper,
) {
    val logger = this.logger()
    route("/inntektsmelding") {
        get("/{inntektsmeldingId}") {
            val inntektsmeldingId =
                call.parameters["inntektsmeldingId"] ?: throw IllegalArgumentException(
                    "Forventet inntektsmeldingId som path parameter",
                )
            logger.info("Fikk request om å hente inntektsmelding med id: $inntektsmeldingId")
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
        }
    }
}
