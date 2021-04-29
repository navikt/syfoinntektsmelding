package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.utsattoppgave.*
import org.apache.kafka.clients.consumer.ConsumerRecords
//import no.nav.helse.inntektsmeldingsvarsel.ANTALL_INNKOMMENDE_MELDINGER
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*


class UtsattOppgaveKafkaClient(props: MutableMap<String, Any>, topicName: String,
                               private val om : ObjectMapper, val oppgaveService: UtsattOppgaveService, private val bakgrunnsjobbRepo: BakgrunnsjobbRepository) :
        ManglendeInntektsmeldingMeldingProvider,
        LivenessComponent {

    private var currentBatch: List<String> = emptyList()
    private var lastThrown: Exception? = null
    private val consumer: KafkaConsumer<String, String>
    private val  topicPartition = TopicPartition(topicName, 0)

    private val log = LoggerFactory.getLogger(UtsattOppgaveKafkaClient::class.java)

    init {

        consumer = KafkaConsumer<String, String>(props, StringDeserializer(), StringDeserializer())
        consumer.assign(Collections.singletonList(topicPartition))

        Runtime.getRuntime().addShutdownHook(Thread {
            log.debug("Got shutdown message, closing Kafka connection...")
            consumer.close()
            log.debug("Kafka connection closed")
        })
    }

    fun stop() = consumer.close()

    override fun getMessagesToProcess(): List<String> {
        if (currentBatch.isNotEmpty()) {
            return currentBatch
        }

        try {
            val records : ConsumerRecords<String, String>? = consumer.poll(Duration.ofMillis(100))
            val payloads = records?.map { it.value() }
            payloads.let {  currentBatch = it!! }
            records?.forEach { record ->
                MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, UUID.randomUUID().toString())
                val hendelse = om.readValue<UtsattOppgaveDTO>(record.value().toString())
                if (DokumentTypeDTO.Inntektsmelding != hendelse.dokumentType) {
                    return@forEach
                }

                try {
                    oppgaveService.prosesser(
                        OppgaveOppdatering(
                            hendelse.dokumentId,
                            hendelse.oppdateringstype.tilHandling(),
                            hendelse.timeout
                        )
                    )
                } catch (ex: Exception) {
                    bakgrunnsjobbRepo.save(
                        Bakgrunnsjobb(
                            type = FeiletUtsattOppgaveMeldingProsessor.JOB_TYPE,
                            kjoeretid = LocalDateTime.now().plusMinutes(30),
                            maksAntallForsoek = 10,
                            data = om.writeValueAsString(
                                FeiletUtsattOppgaveMeldingProsessor.JobbData(
                                    UUID.randomUUID(),
                                    om.writeValueAsString(hendelse)
                                )
                            )
                        )
                    )
                }
                MDCOperations.remove(MDCOperations.MDC_CALL_ID)
            }

            lastThrown = null

            log.debug("Fikk ${records?.count()} meldinger med offsets ${records?.map { it.offset() }?.joinToString(", ")}")
            return payloads!!
        } catch (e: Exception) {
            lastThrown = e
            throw e
        }
    }

    override fun confirmProcessingDone() {
        consumer.commitSync()
//        ANTALL_INNKOMMENDE_MELDINGER.inc(currentBatch.size.toDouble())
        currentBatch = emptyList()
    }

    override suspend fun runLivenessCheck() {
        lastThrown?.let { throw lastThrown as Exception }
    }
}

