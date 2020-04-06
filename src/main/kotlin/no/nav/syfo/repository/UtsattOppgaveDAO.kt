package no.nav.syfo.repository

import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.service.PlanlagtOppgave
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class UtsattOppgaveDAO(
    private val utsattOppgaveRepository: UtsattOppgaveRepository
) {

    fun opprett(arkivreferanse: String, timeout: LocalDateTime = LocalDateTime.now().plusHours(1)) {
        utsattOppgaveRepository.saveAndFlush(UtsattOppgaveEntitet(arkivreferanse = arkivreferanse))
    }

    fun slett(arkivreferanse: String) {
        utsattOppgaveRepository.deleteByArkivreferanse(arkivreferanse)
    }

    fun finn(arkivreferanse: String) =
        utsattOppgaveRepository.findByArkivreferanse(arkivreferanse)
            ?.let { PlanlagtOppgave(it.arkivreferanse) }

}
