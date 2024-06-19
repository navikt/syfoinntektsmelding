package no.nav.syfo.prosesser

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.opprettOppgaveIGosys
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

class FinnAlleUtgaandeOppgaverProcessor(
    private val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val metrikk: Metrikk,
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val om: ObjectMapper
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofMinutes(10).toMillis()) {
    private val logger = this.logger()
    private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")

    override fun doJob(): Unit = MdcUtils.withCallIdAsUuid {
        utsattOppgaveDAO
            .finnAlleUtgåtteOppgaver()
            .mapNotNull {
                val inntektsmeldingEntitet = inntektsmeldingRepository.findByUuid(it.inntektsmeldingId)
                if (inntektsmeldingEntitet == null) {
                    sikkerlogger.error("Fant ikke inntektsmelding for utsatt oppgave: ${it.arkivreferanse}")
                    logger.error("Fant ikke inntektsmelding for utsatt oppgave: ${it.arkivreferanse}")
                    null
                } else {
                    it to inntektsmeldingEntitet
                }
            }
            .forEach { (oppgaveEntitet, inntektsmeldingEntitet) ->
                try {
                    logger.info("Skal opprette oppgave for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                    opprettOppgaveIGosys(
                        oppgaveEntitet,
                        oppgaveClient,
                        utsattOppgaveDAO,
                        behandlendeEnhetConsumer,
                        oppgaveEntitet.speil,
                        inntektsmeldingEntitet,
                        om
                    )
                    oppgaveEntitet.tilstand = Tilstand.OpprettetTimeout
                    oppgaveEntitet.oppdatert = LocalDateTime.now()
                    metrikk.tellUtsattOppgave_OpprettTimeout()
                    utsattOppgaveDAO.lagre(oppgaveEntitet)
                    logger.info("Oppgave opprettet i gosys pga timeout for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                } catch (e: OpprettOppgaveException) {
                    sikkerlogger.error("Feilet ved opprettelse av oppgave ved timeout i gosys for inntektsmelding: ${oppgaveEntitet.arkivreferanse}", e)
                    logger.error("Feilet ved opprettelse av oppgave ved timeout i gosys for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                }
            }
    }
}
