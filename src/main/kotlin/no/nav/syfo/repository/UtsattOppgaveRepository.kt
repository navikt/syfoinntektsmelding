package no.nav.syfo.repository

import no.nav.syfo.dto.UtsattOppgaveEntitet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UtsattOppgaveRepository : JpaRepository<UtsattOppgaveEntitet, String> {
    fun findByInntektsmeldingId(arkivreferanse: String): UtsattOppgaveEntitet?
}
