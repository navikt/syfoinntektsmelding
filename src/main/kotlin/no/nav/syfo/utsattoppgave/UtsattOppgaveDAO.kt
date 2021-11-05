package no.nav.syfo.utsattoppgave

import java.time.LocalDateTime
import log
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository

class UtsattOppgaveDAO(private val utsattOppgaveRepository: UtsattOppgaveRepository) {

    val log = log()

    fun opprett(oppgave: UtsattOppgaveEntitet): Int {
        return utsattOppgaveRepository.opprett(oppgave).id
    }

    fun finn(id: String) =
        utsattOppgaveRepository.findByInntektsmeldingId(id)

    fun lagre(oppgave: UtsattOppgaveEntitet) {
        utsattOppgaveRepository.oppdater(oppgave)
    }

    fun finnAlleUtgåtteOppgaver(): List<UtsattOppgaveEntitet> =
        utsattOppgaveRepository.findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(LocalDateTime.now(), Tilstand.Utsatt)
            .also { log.info("Fant ${it.size} utsatte oppgaver som har timet ut hvor vi skal opprette en oppgave i gosys") }
}
