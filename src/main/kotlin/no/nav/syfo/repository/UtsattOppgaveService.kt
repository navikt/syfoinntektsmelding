package no.nav.syfo.repository

import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.helpers.log
import no.nav.syfo.service.FremtidigOppgave
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class UtsattOppgaveService(
    private val utsattOppgaveRepository: UtsattOppgaveRepository
) {

    fun prosesser(oppdatering: OppgaveOppdatering) {
        val oppgave = finn(oppdatering.id)
        if (oppgave == null) {
            log.warn("Mottok oppdatering på en ukjent oppgave")
            return
        }

    }

    fun opprett(oppgave: FremtidigOppgave) {
        utsattOppgaveRepository.saveAndFlush(
            UtsattOppgaveEntitet(
                fnr = oppgave.fnr,
                sakId = oppgave.saksId,
                aktørId = oppgave.aktørId,
                journalpostId = oppgave.journalpostId,
                arkivreferanse = oppgave.arkivreferanse,
                timeout = oppgave.timeout,
                inntektsmeldingId = oppgave.inntektsmeldingId.toString()
            )
        )
    }

    fun finn(id: UUID) =
        utsattOppgaveRepository.findByInntektsmeldingId(id.toString())
            ?.let {
                FremtidigOppgave(
                    fnr = it.fnr,
                    saksId = it.sakId,
                    aktørId = it.aktørId,
                    journalpostId = it.journalpostId,
                    arkivreferanse = it.arkivreferanse,
                    inntektsmeldingId = UUID.fromString(it.inntektsmeldingId),
                    timeout = it.timeout
                )
            }

    fun oppdater(oppdatering: OppgaveOppdatering, oppgave: FremtidigOppgave) {

    }

}

class OppgaveOppdatering(
    val id: UUID,
    val handling: Handling,
    val timeout: LocalDateTime?
) {

}

enum class Handling {
    Utsett, Opprett, Forkast
}
