package no.nav.syfo.repository

import no.nav.syfo.dto.InntektsmeldingDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InntektsmeldingRepository : JpaRepository<InntektsmeldingDto, String> {

    fun findByAktorId(aktoerId: String): List<InntektsmeldingDto>

}
