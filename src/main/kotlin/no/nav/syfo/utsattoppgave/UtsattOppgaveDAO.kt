package no.nav.syfo.utsattoppgave

import log
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository
import java.time.LocalDateTime

class UtsattOppgaveDAO(private val utsattOppgaveRepository: UtsattOppgaveRepository) {

    val log = log()

    fun opprett(oppgave: UtsattOppgaveEntitet): Int {
        return utsattOppgaveRepository.lagreInnteksmelding(oppgave).id
    }

    fun finn(id: String) =
        utsattOppgaveRepository.findByInntektsmeldingId(id)

    fun lagre(oppgave: UtsattOppgaveEntitet) {
        utsattOppgaveRepository.lagreInnteksmelding(oppgave)
    }

    fun finnAlleUtgåtteOppgaver(): List<UtsattOppgaveEntitet> =
        utsattOppgaveRepository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(LocalDateTime.now(), Tilstand.Utsatt)
            .also { log.info("Fant ${it.size} utsatte oppgaver som har timet ut hvor vi skal opprette en oppgave i gosys") }

}
