package no.nav.syfo.utsattoppgave

import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UtsattOppgaveDAO(val utsattOppgaveRepository: UtsattOppgaveRepository) {

    fun opprett(oppgave: UtsattOppgaveEntitet): Int {
        return utsattOppgaveRepository.saveAndFlush(oppgave).id
    }

    fun finn(id: String) =
        utsattOppgaveRepository.findByInntektsmeldingId(id)

    fun lagre(oppgave: UtsattOppgaveEntitet) {
        utsattOppgaveRepository.saveAndFlush(oppgave)
    }

    fun finnAlleUtgåtteOppgaver(): List<UtsattOppgaveEntitet> =
        utsattOppgaveRepository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(LocalDateTime.now(), Tilstand.Utsatt)

}
