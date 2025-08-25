package no.nav.syfo.utsattoppgave

import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.UtsattOppgaveRepository
import java.time.LocalDateTime

class UtsattOppgaveDAO(
    private val utsattOppgaveRepository: UtsattOppgaveRepository,
) {
    private val logger = logger()

    fun opprett(oppgave: UtsattOppgaveEntitet): Int = utsattOppgaveRepository.opprett(oppgave).id

    fun finn(id: String) = utsattOppgaveRepository.findByInntektsmeldingId(id)

    fun lagre(oppgave: UtsattOppgaveEntitet) {
        utsattOppgaveRepository.oppdater(oppgave)
    }

    fun finnAlleUtgåtteOppgaver(): List<UtsattOppgaveEntitet> =
        utsattOppgaveRepository
            .findUtsattOppgaveEntitetByTimeoutBeforeAndTilstandEquals(LocalDateTime.now(), Tilstand.Utsatt)
            .also { logger.info("Fant ${it.size} utsatte oppgaver som har timet ut hvor vi skal opprette en oppgave i gosys") }
}
