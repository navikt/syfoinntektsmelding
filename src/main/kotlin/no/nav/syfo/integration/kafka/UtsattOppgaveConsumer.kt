package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helsearbeidsgiver.utils.log.MdcUtils
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.util.LivenessComponent
import no.nav.syfo.util.ReadynessComponent
import no.nav.syfo.utsattoppgave.DokumentTypeDTO
import no.nav.syfo.utsattoppgave.FeiletUtsattOppgaveMeldingProsessor
import no.nav.syfo.utsattoppgave.OppgaveOppdatering
import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import no.nav.syfo.utsattoppgave.UtsattOppgaveService
import no.nav.syfo.utsattoppgave.tilHandling
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.time.LocalDateTime

class UtsattOppgaveConsumer(
    props: Map<String, Any>,
    topicName: String,
    val om: ObjectMapper,
    val utsattOppgaveService: UtsattOppgaveService,
    val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
) : ReadynessComponent,
    LivenessComponent {
    private val consumer: KafkaConsumer<String, String> = KafkaConsumer(props, StringDeserializer(), StringDeserializer())
    private val logger = this.logger()
    private val sikkerlogger = sikkerLogger()
    private var ready = false
    private var error = false

    init {
        logger.info("Subscriber til topic $topicName ...")
        consumer.subscribe(listOf(topicName))
    }

    fun setIsReady(ready: Boolean) {
        this.ready = ready
    }

    fun setIsError(isError: Boolean) {
        this.error = isError
    }

    fun start() {
        logger.info("Starter å lytte på UtsattOppgave...")
        consumer.use {
            setIsReady(true)
            while (!error) {
                it
                    .poll(Duration.ofMillis(1000))
                    .forEach { record ->
                        try {
                            val raw: String = record.value()
                            MdcUtils.withCallId {
                                val utsattOppgaveDTO = om.readValue<UtsattOppgaveDTO>(raw)
                                if (utsattOppgaveDTO.dokumentType == DokumentTypeDTO.Inntektsmelding) {
                                    behandle(utsattOppgaveDTO, raw)
                                }
                            }
                            it.commitSync()
                        } catch (e: Throwable) {
                            "Klarte ikke behandle UtsattOppgave. Stopper lytting!".also {
                                logger.error(it)
                                sikkerlogger.error(it, e)
                            }
                            setIsError(true)
                        }
                    }
            }
        }
    }

    fun behandle(
        hendelse: UtsattOppgaveDTO,
        raw: String,
    ) {
        try {
            logger.info("Behandler UtsattOppgave...")
            utsattOppgaveService.prosesser(
                OppgaveOppdatering(
                    hendelse.dokumentId,
                    hendelse.oppdateringstype.tilHandling(),
                    hendelse.timeout,
                    hendelse.oppdateringstype,
                ),
            )
        } catch (ex: Exception) {
            logger.info("Det oppstod en feil ved behandling av UtsattOppgave. Oppretter bakgrunnsjobb.")
            bakgrunnsjobbRepo.save(
                Bakgrunnsjobb(
                    type = FeiletUtsattOppgaveMeldingProsessor.JOB_TYPE,
                    kjoeretid = LocalDateTime.now().plusMinutes(30),
                    maksAntallForsoek = 10,
                    data = raw,
                ),
            )
        }
    }

    override suspend fun runReadynessCheck() {
        if (!ready) {
            throw IllegalStateException("Lytting er ikke klar ennå")
        }
    }

    override suspend fun runLivenessCheck() {
        if (error) {
            throw IllegalStateException("Det har oppstått en feil og slutter å lytte")
        }
    }
}
