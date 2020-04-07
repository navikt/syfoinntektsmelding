package no.nav.syfo.repository

import no.nav.syfo.dto.InntektsmeldingEntitet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface InntektsmeldingRepository : JpaRepository<InntektsmeldingEntitet, String> {

    fun findByAktorId(aktoerId: String): List<InntektsmeldingEntitet>

    fun findFirst100ByBehandletBefore(førDato: LocalDateTime): List<InntektsmeldingEntitet>

    fun deleteByBehandletBefore(førDato: LocalDateTime): List<InntektsmeldingEntitet>

}
