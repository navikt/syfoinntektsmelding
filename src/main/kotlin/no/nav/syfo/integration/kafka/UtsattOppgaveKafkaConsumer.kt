package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.MDCOperations.Companion.MDC_CALL_ID
import no.nav.syfo.utsattoppgave.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Duration
import java.time.LocalDateTime
import java.util.*


class UtsattOppgaveKafkaConsumer(
    props: Map<String, Any>,
    private val topicName: String,
    private val om: ObjectMapper,
    consumerFactory: ConsumerFactory<String, Any>,
    val oppgaveService: UtsattOppgaveService,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
) {
    private val consumer = consumerFactory.createConsumer(props)

    fun consumeMessage() {
        consumer.subscribe(listOf(topicName))
        while (true) {
            val records: ConsumerRecords<String, Any>? = consumer.poll(Duration.ofMillis(100))
            records?.forEach { record ->
                MDCOperations.putToMDC(MDC_CALL_ID, UUID.randomUUID().toString())
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
                MDCOperations.remove(MDC_CALL_ID)
            }
            consumer.commitSync()
        }
    }
}
