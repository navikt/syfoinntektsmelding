package no.nav.syfo.bakgrunnsjobb

import no.nav.syfo.dto.BakgrunnsjobbEntitet
import no.nav.syfo.dto.BakgrunnsjobbStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BakgrunnsjobbRepository : JpaRepository<BakgrunnsjobbEntitet, String> {
    fun findByKjoeretidBeforeAndStatusIn(timeout: LocalDateTime, tilstander: Set<BakgrunnsjobbStatus>): List<BakgrunnsjobbEntitet>
}
