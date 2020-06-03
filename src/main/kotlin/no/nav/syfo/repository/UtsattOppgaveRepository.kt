package no.nav.syfo.repository

import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UtsattOppgaveRepository : JpaRepository<UtsattOppgaveEntitet, String> {
    fun findByInntektsmeldingId(inntektsmeldingId: String): UtsattOppgaveEntitet?
    fun findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(timeout: LocalDateTime, tilstand: Tilstand): List<UtsattOppgaveEntitet>
}
