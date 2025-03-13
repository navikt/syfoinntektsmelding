package no.nav.syfo.prosesser

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.syfo.behandling.OpprettOppgaveException
import no.nav.syfo.client.oppgave.OppgaveService
import no.nav.syfo.dto.Tilstand
import no.nav.syfo.repository.InntektsmeldingRepository
import no.nav.syfo.service.BehandlendeEnhetConsumer
import no.nav.syfo.util.Metrikk
import no.nav.syfo.utsattoppgave.BehandlingsKategori
import no.nav.syfo.utsattoppgave.UtsattOppgaveDAO
import no.nav.syfo.utsattoppgave.hentInntektsmelding
import no.nav.syfo.utsattoppgave.opprettOppgaveIGosys
import no.nav.syfo.utsattoppgave.utledBehandlingsKategori
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

class FinnAlleUtgaandeOppgaverProcessor(
    private val utsattOppgaveDAO: UtsattOppgaveDAO,
    private val oppgaveService: OppgaveService,
    private val behandlendeEnhetConsumer: BehandlendeEnhetConsumer,
    private val metrikk: Metrikk,
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val om: ObjectMapper,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofHours(1).toMillis()) {
    private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")

    override fun doJob(): Unit =
        MdcUtils.withCallIdAsUuid {
            logger.info("Finner alle utgåtte oppgaver")
            utsattOppgaveDAO
                .finnAlleUtgåtteOppgaver()
                .forEach { oppgaveEntitet ->
                    inntektsmeldingRepository
                        .hentInntektsmelding(oppgaveEntitet, om)
                        .onFailure {
                            sikkerlogger.error("Fant ikke inntektsmelding for utsatt oppgave: ${oppgaveEntitet.arkivreferanse}", it)
                            logger.error("Fant ikke inntektsmelding for utsatt oppgave: ${oppgaveEntitet.arkivreferanse}")
                            return@forEach
                        }.onSuccess { inntektsmelding ->

                            val gjelderUtland = behandlendeEnhetConsumer.gjelderUtland(oppgaveEntitet)
                            val behandlingsKategori = utledBehandlingsKategori(oppgaveEntitet, inntektsmelding, gjelderUtland)
                            try {
                                if (behandlingsKategori != BehandlingsKategori.IKKE_FRAVAER) {
                                    logger.info("Skal opprette oppgave for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                                    opprettOppgaveIGosys(
                                        oppgaveEntitet,
                                        oppgaveService,
                                        utsattOppgaveDAO,
                                        behandlingsKategori,
                                    )
                                    logger.info(
                                        "Oppgave opprettet i gosys pga timeout for inntektsmelding: ${oppgaveEntitet.arkivreferanse}",
                                    )
                                    oppgaveEntitet.tilstand = Tilstand.OpprettetTimeout
                                } else {
                                    logger.info(
                                        "Skal ikke opprette oppgave ved timeout for inntektsmelding: ${oppgaveEntitet.arkivreferanse}",
                                    )
                                    sikkerlogger.info(
                                        "Skal ikke opprette oppgave ved timeout for inntektsmelding: ${oppgaveEntitet.arkivreferanse} grunnet ${behandlingsKategori.name}",
                                    )
                                    oppgaveEntitet.tilstand = Tilstand.Forkastet
                                }
                                oppgaveEntitet.oppdatert = LocalDateTime.now()
                                metrikk.tellUtsattOppgaveOpprettTimeout()
                                utsattOppgaveDAO.lagre(oppgaveEntitet)
                            } catch (e: OpprettOppgaveException) {
                                sikkerlogger.error(
                                    "Feilet ved opprettelse av oppgave ved timeout i gosys for inntektsmelding: ${oppgaveEntitet.arkivreferanse}",
                                    e,
                                )
                                logger.error(
                                    "Feilet ved opprettelse av oppgave ved timeout i gosys for inntektsmelding: ${oppgaveEntitet.arkivreferanse}",
                                )
                            }
                        }
                }
        }
}
