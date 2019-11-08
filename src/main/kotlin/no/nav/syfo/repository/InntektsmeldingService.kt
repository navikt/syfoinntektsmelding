package no.nav.syfo.repository

import lombok.extern.slf4j.Slf4j
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingDto
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingDTO
import org.springframework.stereotype.Service

@Service
@Slf4j
class InntektsmeldingService (
    private val repository: InntektsmeldingRepository
) {

    fun opprett(inntektsmelding: Inntektsmelding): String {
        val dto = toInntektsmeldingDTO(inntektsmelding)
        return repository.save(dto).uuid!!
    }

    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<Inntektsmelding> {
        val liste = repository.findByAktorId(aktoerId)
        return liste.map{ InntektsmeldingMeta -> toInntektsmelding(InntektsmeldingMeta) }
    }

    fun lagreBehandling(inntektsmelding: Inntektsmelding, aktorid: String, saksId: String, arkivReferanse: String): InntektsmeldingDto {
        val dto = toInntektsmeldingDTO(inntektsmelding)
        dto.aktorId = aktorid
        dto.sakId = saksId
        return repository.save(dto)
    }

}
