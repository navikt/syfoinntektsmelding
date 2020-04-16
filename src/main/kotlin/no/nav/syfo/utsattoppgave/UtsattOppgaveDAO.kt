package no.nav.syfo.utsattoppgave

import log
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UtsattOppgaveDAO(val utsattOppgaveRepository: UtsattOppgaveRepository) {

    val log = log()

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
            .also { log.info("fant ${it.size} oppgaver som har gått ut") }

}
