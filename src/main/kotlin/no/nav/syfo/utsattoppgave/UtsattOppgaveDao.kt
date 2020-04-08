package no.nav.syfo.utsattoppgave

import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository
import org.springframework.stereotype.Service

@Service
class UtsattOppgaveDao(val utsattOppgaveRepository: UtsattOppgaveRepository) {

    fun opprett(oppgave: UtsattOppgaveEntitet): Int {
        return utsattOppgaveRepository.saveAndFlush(oppgave).id
    }

    fun finn(id: String) =
        utsattOppgaveRepository.findByInntektsmeldingId(id)

    fun save(oppgave: UtsattOppgaveEntitet) {
        oppgave.arkivreferanse = "LOOOL"
        utsattOppgaveRepository.saveAndFlush(oppgave)
    }
}
