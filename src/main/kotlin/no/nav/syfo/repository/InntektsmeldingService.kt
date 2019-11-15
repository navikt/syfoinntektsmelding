package no.nav.syfo.repository

import lombok.extern.slf4j.Slf4j
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingDTO
import org.springframework.stereotype.Service

@Service
@Slf4j
class InntektsmeldingService (
    private val repository: InntektsmeldingRepository
) {

    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<Inntektsmelding> {
        val liste = repository.findByAktorId(aktoerId)
        return liste.map{ InntektsmeldingMeta -> toInntektsmelding(InntektsmeldingMeta) }
    }

    fun lagreBehandling(inntektsmelding: Inntektsmelding, aktorid: String, saksId: String, arkivReferanse: String): InntektsmeldingEntitet {
        val dto = toInntektsmeldingDTO(inntektsmelding)
        dto.aktorId = aktorid
        dto.sakId = saksId
        return repository.save(dto)
    }

    fun lagre(aktoerId: String, saksId: String, journalpostId:String) {
        val dto = InntektsmeldingEntitet(aktorId=aktoerId, sakId=saksId, journalpostId=journalpostId)
        repository.saveAndFlush(dto)
    }

}
