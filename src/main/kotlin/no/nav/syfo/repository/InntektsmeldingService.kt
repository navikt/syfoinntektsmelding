package no.nav.syfo.repository

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingEntitet
import org.slf4j.LoggerFactory

class InntektsmeldingService(
    private val repository: InntektsmeldingRepository,
    private val objectMapper: ObjectMapper
) {
    val log = LoggerFactory.getLogger(InntektsmeldingService::class.java)

    fun finnBehandledeInntektsmeldinger(fnr: String): List<Inntektsmelding> {
        val liste = repository.findByFnr(fnr)
        return liste.map { InntektsmeldingMeta -> toInntektsmelding(InntektsmeldingMeta) }
    }

    fun lagreBehandling(
        inntektsmelding: Inntektsmelding,
        aktorid: String,
        saksId: String
    ): InntektsmeldingEntitet {
        val dto = toInntektsmeldingEntitet(inntektsmelding)
        dto.aktorId = aktorid
        dto.sakId = saksId
        dto.data = inntektsmelding.asJsonString(objectMapper)
        return repository.lagreInnteksmelding(dto)
    }
}

fun Inntektsmelding.asJsonString(objectMapper: ObjectMapper): String {
    return objectMapper.writeValueAsString(this)
}
