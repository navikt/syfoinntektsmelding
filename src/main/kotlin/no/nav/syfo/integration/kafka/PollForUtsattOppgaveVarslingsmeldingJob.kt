package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.utsattoppgave.DokumentTypeDTO
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.utsattoppgave.OppgaveOppdatering
import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import no.nav.syfo.utsattoppgave.tilHandling
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

class PollForUtsattOppgaveVarslingsmeldingJob(
    private val kafkaProvider: UtsattOppgaveKafkaClient,
    private val om: ObjectMapper,
    private val oppgaveService: UtsattOppgaveService,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30),
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue.toMillis()) {

    override fun doJob() {
        do {
            val wasEmpty = kafkaProvider
                .getMessagesToProcess()
                .onEach {
                    MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, UUID.randomUUID().toString())
                    val hendelse = om.readValue<UtsattOppgaveDTO>(it)
                    if (DokumentTypeDTO.Inntektsmelding != hendelse.dokumentType) {
                        return@onEach
                    }

                    try {
                        oppgaveService.prosesser(
                            OppgaveOppdatering(
                                hendelse.dokumentId,
                                hendelse.oppdateringstype.tilHandling(),
                                hendelse.timeout,
                                hendelse.oppdateringstype
                            )
                        )
                    } catch (ex: Exception) {
                        bakgrunnsjobbRepo.save(
                            Bakgrunnsjobb(
                                type = FeiletUtsattOppgaveMeldingProsessor.JOB_TYPE,
                                kjoeretid = LocalDateTime.now().plusMinutes(30),
                                maksAntallForsoek = 10,
                                data = it
                            )
                        )
                    }
                    MDCOperations.remove(MDCOperations.MDC_CALL_ID)
                }
                .isEmpty()

            if (!wasEmpty) {
                kafkaProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }
}
