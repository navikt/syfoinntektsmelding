package no.nav.syfo.prosesser

import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.opprettOppgaveIGosys
import org.slf4j.LoggerFactory
import java.util.*

class FinnAlleUtgaandeOppgaverProcessor(
    private  val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer
) : BakgrunnsjobbProsesserer {
    val log = LoggerFactory.getLogger(FinnAlleUtgaandeOppgaverProcessor::class.java)!!
    companion object { const val JOB_TYPE = "finn-alle-utgående-oppgaver"}
    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
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



    data class JobbData(val id: UUID)

}
