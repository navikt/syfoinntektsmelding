package no.nav.syfo.repository

import no.nav.syfo.dto.InntektsmeldingEntitet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InntektsmeldingRepository : JpaRepository<InntektsmeldingEntitet, String> {

    fun findByAktorId(aktoerId: String): List<InntektsmeldingEntitet>

}
