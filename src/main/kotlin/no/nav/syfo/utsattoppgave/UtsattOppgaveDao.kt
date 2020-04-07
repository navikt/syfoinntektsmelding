package no.nav.syfo.utsattoppgave

import no.nav.syfo.dto.UtsattOppgaveEntitet
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UtsattOppgaveDao(val utsattOppgaveRepository: UtsattOppgaveRepository) {

    fun opprett(oppgave: FremtidigOppgave): Int {
        return utsattOppgaveRepository.saveAndFlush(
            UtsattOppgaveEntitet(
                fnr = oppgave.fnr,
                sakId = oppgave.saksId,
                aktørId = oppgave.aktørId,
                journalpostId = oppgave.journalpostId,
                arkivreferanse = oppgave.arkivreferanse,
                timeout = oppgave.timeout,
                inntektsmeldingId = oppgave.inntektsmeldingId.toString(),
                tilstand = oppgave.tilstand.name
            )
        ).id
    }

    fun finn(id: UUID) =
        utsattOppgaveRepository.findByInntektsmeldingId(id.toString())
            ?.let {
                FremtidigOppgave(
                    id = it.id,
                    fnr = it.fnr,
                    saksId = it.sakId,
                    aktørId = it.aktørId,
                    journalpostId = it.journalpostId,
                    arkivreferanse = it.arkivreferanse,
                    inntektsmeldingId = UUID.fromString(it.inntektsmeldingId),
                    timeout = it.timeout,
                    tilstand = Tilstand.valueOf(it.tilstand)
                )
            }

    fun save(oppgave: FremtidigOppgave) {
        utsattOppgaveRepository.saveAndFlush(
            UtsattOppgaveEntitet(
                id = oppgave.id,
                fnr = oppgave.fnr,
                sakId = oppgave.saksId,
                aktørId = oppgave.aktørId,
                journalpostId = oppgave.journalpostId,
                arkivreferanse = oppgave.arkivreferanse,
                timeout = oppgave.timeout,
                inntektsmeldingId = oppgave.inntektsmeldingId.toString(),
                tilstand = oppgave.tilstand.name
            )
        )
    }
}
