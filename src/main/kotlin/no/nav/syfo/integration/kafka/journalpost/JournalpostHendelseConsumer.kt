package no.nav.syfo.integration.kafka.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.kubernetes.LivenessComponent
import no.nav.helse.arbeidsgiver.kubernetes.ReadynessComponent
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.DuplikatRepository
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration.ofMillis
import java.time.LocalDateTime

class JournalpostHendelseConsumer(
    props: Map<String, Any>,
    topicName: String,
    private val duplikatRepository: DuplikatRepository,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val om: ObjectMapper
) : ReadynessComponent, LivenessComponent {

    private val log = LoggerFactory.getLogger(JournalpostHendelseConsumer::class.java)
    private val consumer: KafkaConsumer<String, GenericRecord> = KafkaConsumer(props)
    private var ready = false
    private var error = false

    init {
        log.info("Lytter på topic $topicName")
        consumer.subscribe(listOf(topicName))
    }

    fun setIsReady(ready: Boolean){
        this.ready = ready
    }

    fun setIsError(isError: Boolean){
        this.error = isError
    }

    fun start() {
        var totalCount = 0L
        log.info("Starter...")
        consumer.use {
            setIsReady(true)
            while (!error) {
                consumer
                    .poll(ofMillis(1000))
                    .fold(totalCount) { accumulator, record ->
                        val newCount = accumulator + 1
                        try {
                            try {
                                processHendelse(mapJournalpostHendelse(record.value()))
                            } catch (ex: Throwable) {
                                throw IllegalArgumentException("Klarte ikke lese journalposthendelse med offset ${record.offset()}!", ex)
                            }
                            consumer.commitSync()
                        } catch (e: Throwable) {
                            log.error("Klarte ikke behandle Journalpost. Stopper lytting!", e)
                            setIsError(true)
                        }
                        newCount
                    }
            }
        }
    }

    fun processHendelse(journalpostDTO: InngaaendeJournalpostDTO): Int {
        if (isInntektsmelding(journalpostDTO)) {
            if (isDuplicate(journalpostDTO)) {
                log.info("Ignorerer duplikat inntektsmelding ${journalpostDTO.kanalReferanseId} for hendelse ${journalpostDTO.hendelsesId}")
                return 0
            } else {
                log.info("Fant inntektsmelding ${journalpostDTO.kanalReferanseId} for hendelse ${journalpostDTO.hendelsesId}")
                lagreBakgrunnsjobb(journalpostDTO)
                return 1
            }
        } else {
            log.info("Ignorerte hendelse ${journalpostDTO.hendelsesId}. Kanal: ${journalpostDTO.mottaksKanal} Tema: ${journalpostDTO.temaNytt} Status: ${journalpostDTO.journalpostStatus}")
            return -1
        }
    }

    fun lagreBakgrunnsjobb(hendelse: InngaaendeJournalpostDTO) {
        bakgrunnsjobbRepo.save(
            Bakgrunnsjobb(
                type = JoarkInntektsmeldingHendelseProsessor.JOB_TYPE,
                kjoeretid = LocalDateTime.now(),
                maksAntallForsoek = 10,
                data = om.writeValueAsString(hendelse)
            )
        )
    }

    fun isDuplicate(hendelse: InngaaendeJournalpostDTO): Boolean {
        return duplikatRepository.findByHendelsesId(hendelse.hendelsesId)
    }

    override suspend fun runReadynessCheck() {
        if (!ready) {
            throw IllegalStateException("Not started yet.")
        }
    }

    override suspend fun runLivenessCheck() {
        if (!ready || error) {
            throw IllegalStateException("Failed to read from")
        }
    }
}

fun isInntektsmelding(hendelse: InngaaendeJournalpostDTO): Boolean {
    return hendelse.temaNytt == "SYK" && hendelse.mottaksKanal == "ALTINN" && hendelse.journalpostStatus == "MOTTATT"
}
