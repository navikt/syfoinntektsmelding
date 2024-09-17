package no.nav.syfo.integration.kafka.journalpost

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.util.LivenessComponent
import no.nav.syfo.util.ReadynessComponent
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration.ofMillis
import java.time.LocalDateTime

enum class JournalpostStatus {
    Ny,
    IkkeInntektsmelding,
    FeilHendelseType,
}

class JournalpostHendelseConsumer(
    props: Map<String, Any>,
    topicName: String,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val om: ObjectMapper
) : ReadynessComponent, LivenessComponent {

    private val log = LoggerFactory.getLogger(JournalpostHendelseConsumer::class.java)
    private val sikkerlogger = sikkerLogger()
    private val consumer: KafkaConsumer<String, GenericRecord> = KafkaConsumer(props)
    private var ready = false
    private var error = false

    init {
        log.info("Lytter på topic $topicName")
        consumer.subscribe(listOf(topicName))
    }

    fun setIsReady(ready: Boolean) {
        this.ready = ready
    }

    fun setIsError(isError: Boolean) {
        this.error = isError
    }

    fun start() {
        log.info("Starter...")
        consumer.use {
            setIsReady(true)
            while (!error) {
                it.poll(ofMillis(1000)).forEach { record ->
                    try {
                        processHendelse(mapJournalpostHendelse(record.value()))
                        it.commitSync()
                    } catch (e: Throwable) {
                        sikkerlogger.error("Klarte ikke behandle hendelse. Stopper lytting!", e)
                        setIsError(true)
                    }
                }
            }
        }
    }

    fun processHendelse(journalpostDTO: InngaaendeJournalpostDTO) {
        when (findStatus(journalpostDTO)) {
            JournalpostStatus.Ny -> lagreBakgrunnsjobb(journalpostDTO)
            JournalpostStatus.IkkeInntektsmelding -> log.info(
                "Ignorerte journalposthendelse ${journalpostDTO.hendelsesId}. Kanal: ${journalpostDTO.mottaksKanal} Tema: ${journalpostDTO.temaNytt} Status: ${journalpostDTO.journalpostStatus}"
            )

            JournalpostStatus.FeilHendelseType -> log.info(
                "Ingorerte JournalpostHendelse ${journalpostDTO.hendelsesId} av type ${journalpostDTO.hendelsesType} med referanse: ${journalpostDTO.kanalReferanseId}"
            )
        }
    }

    fun findStatus(journalpostDTO: InngaaendeJournalpostDTO): JournalpostStatus {
        if (isInntektsmelding(journalpostDTO)) {
            if (journalpostDTO.hendelsesType != "JournalpostMottatt") {
                return JournalpostStatus.FeilHendelseType
            }
            return JournalpostStatus.Ny
        }
        return JournalpostStatus.IkkeInntektsmelding
    }

    private fun lagreBakgrunnsjobb(hendelse: InngaaendeJournalpostDTO) {
        log.info("Lagrer inntektsmelding ${hendelse.kanalReferanseId} for hendelse ${hendelse.hendelsesId}")
        bakgrunnsjobbRepo.save(
            Bakgrunnsjobb(
                type = JoarkInntektsmeldingHendelseProsessor.JOB_TYPE,
                kjoeretid = LocalDateTime.now(),
                maksAntallForsoek = 10,
                data = om.writeValueAsString(hendelse)
            )
        )
    }

    override suspend fun runReadynessCheck() {
        if (!ready) {
            throw IllegalStateException("Lytting på hendelser er ikke klar ennå")
        }
    }

    override suspend fun runLivenessCheck() {
        if (error) {
            throw IllegalStateException("Det har oppstått en feil og slutter å lytte på hendelser")
        }
    }
}

fun isInntektsmelding(hendelse: InngaaendeJournalpostDTO): Boolean =
    hendelse.temaNytt == "SYK" && hendelse.mottaksKanal == "ALTINN" && hendelse.journalpostStatus == "MOTTATT"
