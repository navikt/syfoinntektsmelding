package no.nav.syfo.prosesser

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.opprettOppgaveIGosys
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

class FinnAlleUtgaandeOppgaverProcessor(
    private val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val metrikk: Metrikk,
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val om: ObjectMapper
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofHours(6).toMillis()) {
    val log = LoggerFactory.getLogger(FinnAlleUtgaandeOppgaverProcessor::class.java)!!

    override fun doJob() {
        MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, UUID.randomUUID().toString())
//        utsattOppgaveDAO
//            .finnAlleUtgåtteOppgaver()
//            .forEach {
//                try {
//                    log.info("Skal opprette oppgave for inntektsmelding: ${it.arkivreferanse}")
//                    val inntektsmeldingEntitet = inntektsmeldingRepository.findByArkivReferanse(it.arkivreferanse)
//                    opprettOppgaveIGosys(it, oppgaveClient, utsattOppgaveDAO, behandlendeEnhetConsumer, it.speil, inntektsmeldingEntitet, om)
//                    it.tilstand = Tilstand.OpprettetTimeout
//                    it.oppdatert = LocalDateTime.now()
//                    metrikk.tellUtsattOppgave_OpprettTimeout()
//                    utsattOppgaveDAO.lagre(it)
//                    log.info("Oppgave opprettet i gosys pga timeout for inntektsmelding: ${it.arkivreferanse}")
//                } catch (e: OpprettOppgaveException) {
//                    log.error("Feilet ved opprettelse av oppgave ved timeout i gosys for inntektsmelding: ${it.arkivreferanse}")
//                }
//            }
        MDCOperations.remove(MDCOperations.MDC_CALL_ID)
    }
}
