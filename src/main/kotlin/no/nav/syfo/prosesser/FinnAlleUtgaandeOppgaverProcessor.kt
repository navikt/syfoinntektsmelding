package no.nav.syfo.prosesser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.syfo.service.SYKEPENGER_UTLAND
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.BehandlingsKategori
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.finnBehandlingsKategori
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
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofHours(6).toMillis()) {
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
                val inntektsmelding = om.readValue<no.nav.syfo.domain.inntektsmelding.Inntektsmelding>(inntektsmeldingEntitet.data!!)
                val behandlendeEnhet =
                    behandlendeEnhetConsumer.hentBehandlendeEnhet(
                        oppgaveEntitet.fnr,
                        oppgaveEntitet.inntektsmeldingId,
                    )
                val gjelderUtland = (SYKEPENGER_UTLAND == behandlendeEnhet)
                logger.info("Fant enhet $behandlendeEnhet for ${oppgaveEntitet.arkivreferanse}")
                val behandlingsKategori = finnBehandlingsKategori(inntektsmelding, oppgaveEntitet.speil, gjelderUtland)
                try {
                    if (behandlingsKategori != BehandlingsKategori.IKKE_FRAVAER) {
                        logger.info("Skal opprette oppgave for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                        opprettOppgaveIGosys(
                            oppgaveEntitet,
                            oppgaveClient,
                            utsattOppgaveDAO,
                            behandlingsKategori
                        )
                        logger.info("Oppgave opprettet i gosys pga timeout for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                        oppgaveEntitet.tilstand = Tilstand.OpprettetTimeout
                    } else {
                        logger.info("Skal ikke opprette oppgave for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                        sikkerlogger.info("Skal ikke opprette oppgave for inntektsmelding: ${oppgaveEntitet.arkivreferanse} grunnet ${behandlingsKategori.name}")
                        oppgaveEntitet.tilstand = Tilstand.Forkastet
                    }
                    oppgaveEntitet.oppdatert = LocalDateTime.now()
                    metrikk.tellUtsattOppgave_OpprettTimeout()
                    utsattOppgaveDAO.lagre(oppgaveEntitet)
                } catch (e: OpprettOppgaveException) {
                    sikkerlogger.error("Feilet ved opprettelse av oppgave ved timeout i gosys for inntektsmelding: ${oppgaveEntitet.arkivreferanse}", e)
                    logger.error("Feilet ved opprettelse av oppgave ved timeout i gosys for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                }
            }
    }
}
