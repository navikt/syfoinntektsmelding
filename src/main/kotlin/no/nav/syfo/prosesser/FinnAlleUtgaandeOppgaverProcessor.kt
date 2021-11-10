package no.nav.syfo.prosesser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.opprettOppgaveIGosys
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

class FinnAlleUtgaandeOppgaverProcessor(
    private val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofHours(6).toMillis()) {
    val log = LoggerFactory.getLogger(FinnAlleUtgaandeOppgaverProcessor::class.java)!!

    override fun doJob() {
        MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, UUID.randomUUID().toString())
        utsattOppgaveDAO
            .finnAlleUtgåtteOppgaver()
            .forEach {
                try {
                    opprettOppgaveIGosys(it, oppgaveClient, utsattOppgaveDAO, behandlendeEnhetConsumer)
                    it.tilstand = Tilstand.Opprettet
                    utsattOppgaveDAO.lagre(it)
                    log.info("Oppgave opprettet i gosys for inntektsmelding: ${it.inntektsmeldingId}")
                } catch (e: OpprettOppgaveException) {
                    log.error("feil ved opprettelse av oppgave i gosys. InntektsmeldingId: ${it.inntektsmeldingId}")
                }
            }
        MDCOperations.remove(MDCOperations.MDC_CALL_ID)
    }
}
