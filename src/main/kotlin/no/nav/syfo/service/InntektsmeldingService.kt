package no.nav.syfo.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.domain.inntektsmelding.isDuplicate
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingEntitet
import no.nav.syfo.repository.InntektsmeldingRepository
import org.slf4j.LoggerFactory

class InntektsmeldingService(
    private val repository: InntektsmeldingRepository,
    private val objectMapper: ObjectMapper
) {
    val log = LoggerFactory.getLogger(InntektsmeldingService::class.java)

    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<Inntektsmelding> {
        val liste = repository.findByAktorId(aktoerId)
        return liste.map { toInntektsmelding(it) }
    }

    fun isDuplicate(inntektsmelding: Inntektsmelding): Boolean {
        inntektsmelding.aktorId?.let {
            findPresent(inntektsmelding, it).apply {
                log.info("Inntektsmelding finnes fra før $inntektsmelding")
                return true
            }
        }
        return false
    }

    fun findPresent(inntektsmelding: Inntektsmelding, aktoerId: String): Inntektsmelding? {
        finnBehandledeInntektsmeldinger(aktoerId).forEach {
            if (it.isDuplicate(inntektsmelding)) {
                return it
            }
        }
        return null
    }

    fun lagreBehandling(
        inntektsmelding: Inntektsmelding,
        aktorid: String,
        saksId: String,
        arkivReferanse: String
    ): InntektsmeldingEntitet {
        val dto = toInntektsmeldingEntitet(inntektsmelding)
        dto.aktorId = aktorid
        dto.sakId = saksId
        dto.data = inntektsmelding.asJsonString(objectMapper)
        return repository.lagreInnteksmelding(dto)
    }
}

fun Inntektsmelding.asJsonString(objectMapper: ObjectMapper): String {
    val im = this.copy(fnr = "") // La stå! Ikke lagre fødselsnummer
    return objectMapper.writeValueAsString(im)
}
