package no.nav.syfo.utsattoppgave

import no.nav.syfo.dto.UtsattOppgaveEntitet
import org.springframework.stereotype.Service

@Service
class UtsattOppgaveDao(val utsattOppgaveRepository: UtsattOppgaveRepository) {

    fun opprett(oppgave: UtsattOppgaveEntitet): Int {
        return utsattOppgaveRepository.saveAndFlush(oppgave).id
    }

    fun finn(id: String) =
        utsattOppgaveRepository.findByInntektsmeldingId(id)

    fun save(oppgave: UtsattOppgaveEntitet) {
        utsattOppgaveRepository.saveAndFlush(oppgave)
    }
}
