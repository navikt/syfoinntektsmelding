package no.nav.syfo.prosesser

import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbProsesserer
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.consumer.rest.OppgaveClient
import no.nav.syfo.consumer.ws.BehandlendeEnhetConsumer
import no.nav.syfo.consumer.ws.SYKEPENGER_UTLAND
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.dto.UtsattOppgaveEntitet
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.repository.UtsattOppgaveRepository
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class FinnAlleUtgaandeOppgaverProcessor(
    private  val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer
) : BakgrunnsjobbProsesserer {
    val log = LoggerFactory.getLogger(FinnAlleUtgaandeOppgaverProcessor::class.java)
    companion object { val JOB_TYPE = "finn-alle-utgående-oppgaver"}
    override val type: String get() = JOB_TYPE

    override fun prosesser(jobb: Bakgrunnsjobb) {
        MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, UUID.randomUUID().toString())
        utsattOppgaveDAO
            .finnAlleUtgåtteOppgaver()
            .forEach {
                try {
                    opprettOppgaveIGosys(it)
                    it.tilstand = Tilstand.Opprettet
                    lagre(it)
                    log.info("Oppgave opprettet i gosys for inntektsmelding: ${it.inntektsmeldingId}")
                } catch (e: OpprettOppgaveException) {
                    log.error("feil ved opprettelse av oppgave i gosys. InntektsmeldingId: ${it.inntektsmeldingId}")
                }
            }
        MDCOperations.remove(MDCOperations.MDC_CALL_ID)
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
    data class JobbData(val id: UUID)

}
