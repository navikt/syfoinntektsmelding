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

    fun start() {
        var totalCount = 0L
        log.info("Starter...")
        consumer.use {
            ready = true
            while (true && !error) {
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
                            error = true
                        }
                        newCount
                    }
            }
        }
    }

    fun processHendelse(hendelse: InngaaendeJournalpostDTO) {
        if (isInntektsmelding(hendelse)) {
            if (isDuplicate(hendelse)) {
                log.info("Ignorerer duplikat inntektsmelding ${hendelse.kanalReferanseId} for hendelse ${hendelse.hendelsesId}")
            } else {
                log.info("Fant inntektsmelding ${hendelse.kanalReferanseId} for hendelse ${hendelse.hendelsesId}")
                lagreBakgrunnsjobb(hendelse)
            }
        } else {
            log.info("Ignorerte hendelse ${hendelse.hendelsesId}. Kanal: ${hendelse.mottaksKanal} Tema: ${hendelse.temaNytt} Status: ${hendelse.journalpostStatus}")
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

    fun isInntektsmelding(hendelse: InngaaendeJournalpostDTO): Boolean {
        return hendelse.temaNytt == "SYK" && hendelse.mottaksKanal == "ALTINN" && hendelse.journalpostStatus == "MOTTATT"
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
        if (error) {
            throw IllegalStateException("Failed to read from")
        }
    }
}
