package no.nav.syfo.repository

import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.service.FremtidigOppgave
import no.nav.syfo.service.PlanlagtOppgave
import org.springframework.stereotype.Service

@Service
class UtsattOppgaveDAO(
    private val utsattOppgaveRepository: UtsattOppgaveRepository
) {

    fun opprett(oppgave: FremtidigOppgave) {
        utsattOppgaveRepository.saveAndFlush(
            UtsattOppgaveEntitet(
                fnr = oppgave.fnr,
                sakId = oppgave.saksId,
                aktørId = oppgave.aktørId,
                journalpostId = oppgave.journalpostId,
                arkivreferanse = oppgave.arkivreferanse,
                timeout = oppgave.timeout
            )
        )
    }

    fun finn(arkivreferanse: String) =
        utsattOppgaveRepository.findByArkivreferanse(arkivreferanse)
            ?.let { PlanlagtOppgave(it.arkivreferanse) }

}
