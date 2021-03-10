package no.nav.syfo.utsattoppgave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.syfo.dto.BakgrunnsjobbEntitet
import no.nav.syfo.util.MDCOperations
import no.nav.syfo.util.MDCOperations.Companion.MDC_CALL_ID
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())
    .registerModule(KotlinModule())

@Component
class UtsattOppgaveConsumer(
    val oppgaveService: UtsattOppgaveService,
    val bakgrunnsjobbService: BakgrunnsjobbService,
    val om: ObjectMapper) {

    @KtorExperimentalAPI
    @KafkaListener(
        topics = ["aapen-helse-spre-oppgaver"],
        idIsGroup = false,
        containerFactory = "utsattOppgaveListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, UtsattOppgaveDTO>, acknowledgment: Acknowledgment) {
        MDCOperations.putToMDC(MDC_CALL_ID, UUID.randomUUID().toString())
        if (cr.value().dokumentType != DokumentTypeDTO.Inntektsmelding) {
            acknowledgment.acknowledge()
            return
        }

        val utsattOppgave = cr.value()
        val oppdatering = OppgaveOppdatering(
            utsattOppgave.dokumentId,
            utsattOppgave.oppdateringstype.tilHandling(),
            utsattOppgave.timeout
        )

        try {
            oppgaveService.prosesser(oppdatering)
        } catch(ex: Exception) {
            bakgrunnsjobbService.opprett(BakgrunnsjobbEntitet(
                data = om.writeValueAsString(utsattOppgave),
                type = FeiletUtsattOppgaveMeldingProsessor.JOBB_TYPE,
                kjoeretid = LocalDateTime.now().plusMinutes(30),
                maksAntallForsoek = 10
            ))
        }

        acknowledgment.acknowledge()
        MDCOperations.remove(MDC_CALL_ID)
    }

}

fun OppdateringstypeDTO.tilHandling() = when (this) {
    OppdateringstypeDTO.Utsett -> Handling.Utsett
    OppdateringstypeDTO.Opprett -> Handling.Opprett
    OppdateringstypeDTO.Ferdigbehandlet -> Handling.Forkast
}
