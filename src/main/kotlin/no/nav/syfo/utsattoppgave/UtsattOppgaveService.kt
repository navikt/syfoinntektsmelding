package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import log
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.SYKEPENGER_UTLAND
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.prosesser.FinnAlleUtgaandeOppgaverProcessor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@KtorExperimentalAPI
class UtsattOppgaveService(
    val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val objectMapper : ObjectMapper
) {

    val log = log()

    fun opprettOppgaverForUtgåtte() {
        bakgrunnsjobbRepo.save(
            Bakgrunnsjobb(
                type = FinnAlleUtgaandeOppgaverProcessor.JOB_TYPE,
                kjoeretid = LocalDate.now().plusDays(1).atStartOfDay(),
                maksAntallForsoek = 10,
                data = objectMapper.writeValueAsString(FinnAlleUtgaandeOppgaverProcessor.JobbData(UUID.randomUUID()))
            )
        )

    }

    fun prosesser(oppdatering: OppgaveOppdatering) {
        val oppgave = utsattOppgaveDAO.finn(oppdatering.id.toString())
        if (oppgave == null) {
            log.warn("Mottok oppdatering på en ukjent oppgave")
            return
        }

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Utsett) {
            oppdatering.timeout ?: error("Timeout på utsettelse mangler")
            oppgave.timeout = oppdatering.timeout
            lagre(oppgave)
            log.info("Oppdaterte timeout på inntektsmelding: ${oppgave.inntektsmeldingId} til ${oppdatering.timeout}")
            return
        }

        if (oppgave.tilstand == Tilstand.Utsatt && oppdatering.handling == Handling.Forkast) {
            lagre(oppgave.copy(tilstand = Tilstand.Forkastet))
            log.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Forkastet.name}")
            return
        }

        if ((oppgave.tilstand == Tilstand.Utsatt || oppgave.tilstand == Tilstand.Forkastet) && oppdatering.handling == Handling.Opprett) {
            opprettOppgaveIGosys(oppgave)
            lagre(oppgave.copy(tilstand = Tilstand.Opprettet))
            log.info("Endret oppgave: ${oppgave.inntektsmeldingId} til tilstand: ${Tilstand.Opprettet.name}")
            return
        }

        log.info("Oppdatering på dokumentId: ${oppdatering.id} ikke relevant")
    }

    @KtorExperimentalAPI
    fun opprettOppgaveIGosys(utsattOppgave: UtsattOppgaveEntitet) {
        val behandlendeEnhet = behandlendeEnhetConsumer.hentBehandlendeEnhet(utsattOppgave.fnr, utsattOppgave.inntektsmeldingId)
        val gjelderUtland = (SYKEPENGER_UTLAND == behandlendeEnhet)
        runBlocking {
            oppgaveClient.opprettOppgave(
                sakId = utsattOppgave.sakId,
                journalpostId = utsattOppgave.journalpostId,
                tildeltEnhetsnr = behandlendeEnhet,
                aktoerId = utsattOppgave.aktørId,
                gjelderUtland = gjelderUtland
            )
        }
        utsattOppgave.enhet = behandlendeEnhet;
        utsattOppgaveDAO.lagre(utsattOppgave);
    }

    fun lagre(oppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.lagre(oppgave)
    }

    fun opprett(utsattOppgave: UtsattOppgaveEntitet) {
        utsattOppgaveDAO.opprett(utsattOppgave)
    }
}

class OppgaveOppdatering(
    val id: UUID,
    val handling: Handling,
    val timeout: LocalDateTime?
)

enum class Handling {
    Utsett, Opprett, Forkast
}
