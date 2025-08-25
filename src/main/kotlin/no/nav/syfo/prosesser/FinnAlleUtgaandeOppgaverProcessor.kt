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
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofHours(6).toMillis()) {
    private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")

    override fun doJob(): Unit =
        MdcUtils.withCallIdAsUuid {
            logger.info("Finner alle utgåtte oppgaver")
            utsattOppgaveDAO
                .finnAlleUtgåtteOppgaver()
                .forEach { oppgaveEntitet ->
                    inntektsmeldingRepository
                        .hentInntektsmelding(oppgaveEntitet, om)
                        .onFailure { e ->
                            "Fant ikke inntektsmelding for utsatt oppgave: ${oppgaveEntitet.arkivreferanse}".also {
                                logger.error(it)
                                sikkerlogger.error(it, e)
                            }
                            return@forEach
                        }.onSuccess { inntektsmelding ->
                            try {
                                logger.info("Henter behandlende enhet for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                                val gjelderUtland = behandlendeEnhetConsumer.gjelderUtland(oppgaveEntitet)
                                logger.info("Utleder behandlingskategori for inntektsmelding: ${oppgaveEntitet.arkivreferanse}")
                                val behandlingsKategori = utledBehandlingsKategori(oppgaveEntitet, inntektsmelding, gjelderUtland)
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
                                    "Skal ikke opprette oppgave ved timeout for inntektsmelding: ${oppgaveEntitet.arkivreferanse}".also {
                                        logger.info(it)
                                        sikkerlogger.info("$it grunnet ${behandlingsKategori.name}")
                                    }
                                    oppgaveEntitet.tilstand = Tilstand.Forkastet
                                }
                                oppgaveEntitet.oppdatert = LocalDateTime.now()
                                metrikk.tellUtsattOppgaveOpprettTimeout()
                                utsattOppgaveDAO.lagre(oppgaveEntitet)
                            } catch (e: OpprettOppgaveException) {
                                "Feilet ved opprettelse av oppgave ved timeout i gosys for inntektsmelding: ${oppgaveEntitet.arkivreferanse}".also {
                                    logger.error(it, e)
                                    sikkerlogger.error(it, e)
                                }
                            } catch (e: Exception) {
                                "Feilet ved opprettelse av oppgave ved timeout for inntetksmelding: ${oppgaveEntitet.arkivreferanse}".also {
                                    logger.error(it)
                                    sikkerlogger.error(it, e)
                                }
                                throw e
                            }
                        }
                }
        }
}
