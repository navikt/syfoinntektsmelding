package no.nav.syfo.prosesser

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.helse.arbeidsgiver.utils.logger
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.client.OppgaveClient
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.util.MdcUtils
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.opprettOppgaveIGosys
import java.time.Duration
import java.time.LocalDateTime

class FinnAlleUtgaandeOppgaverProcessor(
    private val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveClient: OppgaveClient,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val metrikk: Metrikk,
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val om: ObjectMapper
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofHours(6).toMillis()) {
    private val logger = this.logger()

    override fun doJob(): Unit = MdcUtils.withCallIdAsUuid {
        utsattOppgaveDAO
            .finnAlleUtgåtteOppgaver()
            .forEach {
                try {
                    logger.info("Skal opprette oppgave for inntektsmelding: ${it.arkivreferanse}")
                    val inntektsmeldingEntitet = inntektsmeldingRepository.findByArkivReferanse(it.arkivreferanse)
                    opprettOppgaveIGosys(it, oppgaveClient, utsattOppgaveDAO, behandlendeEnhetConsumer, it.speil, inntektsmeldingEntitet, om)
                    it.tilstand = Tilstand.OpprettetTimeout
                    it.oppdatert = LocalDateTime.now()
                    metrikk.tellUtsattOppgave_OpprettTimeout()
                    utsattOppgaveDAO.lagre(it)
                    logger.info("Oppgave opprettet i gosys pga timeout for inntektsmelding: ${it.arkivreferanse}")
                } catch (e: OpprettOppgaveException) {
                    logger.error("Feilet ved opprettelse av oppgave ved timeout i gosys for inntektsmelding: ${it.arkivreferanse}")
                }
            }
    }
}
