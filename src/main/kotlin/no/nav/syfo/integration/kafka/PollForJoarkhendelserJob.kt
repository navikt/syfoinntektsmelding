package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import no.nav.syfo.repository.DuplikatRepository
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

fun mapInngaaendeJournalpostDTO(record: GenericRecord): InngaaendeJournalpostDTO {
    return InngaaendeJournalpostDTO(
        record.get("hendelsesId") as String,
        record.get("versjon") as Int,
        record.get("hendelsesType") as String,
        record.get("journalpostId") as Long,
        record.get("journalpostStatus") as String,
        record.get("temaGammelt") as String,
        record.get("temaNytt") as String,
        record.get("mottaksKanal") as String,
        record.get("kanalReferanseId") as String,
        record.get("behandlingstema") as String
    )
}

class PollForJoarkhendelserJob(
    private val kafkaProvider: JoarkHendelseKafkaClient,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val duplikatRepository: DuplikatRepository,
    private val om: ObjectMapper,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue.toMillis()) {

    private val log = LoggerFactory.getLogger(PollForJoarkhendelserJob::class.java)
    override fun doJob() {
        do {
            val wasEmpty = processAll(kafkaProvider.getMessagesToProcess())
            if (wasEmpty) {
                kafkaProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }

    fun processAll(journalposter: List<InngaaendeJournalpostDTO>): Boolean {
        return journalposter
            .filter {
                isInntektsmelding(it)
            }
            .filter {
                !isDuplicate(it)
            }
            .onEach {
                // https://confluence.adeo.no/display/BOA/Tema https://confluence.adeo.no/display/BOA/Mottakskanal
                log.info("Fant journalpost AR${it.kanalReferanseId} fra ALTINN for syk med status midlertidig.")
                bakgrunnsjobbRepo.save(
                    Bakgrunnsjobb(
                        type = JoarkInntektsmeldingHendelseProsessor.JOB_TYPE,
                        kjoeretid = LocalDateTime.now(),
                        maksAntallForsoek = 10,
                        data = om.writeValueAsString(it)
                    )
                )
            }
            .isEmpty()
    }

    fun isInntektsmelding(journalpost: InngaaendeJournalpostDTO): Boolean {
        return journalpost.temaNytt == "SYK" && journalpost.mottaksKanal == "ALTINN" && journalpost.journalpostStatus == "MOTTATT"
    }

    fun isDuplicate(journalpost: InngaaendeJournalpostDTO): Boolean {
        val dupe = duplikatRepository.findByHendelsesId(journalpost.hendelsesId)
        if (dupe) {
            log.info("Inntektsmelding er tidligere importert for journalpost ${journalpost.kanalReferanseId} for hendelse ${journalpost.hendelsesId}")
        } else {
            log.info("Fant ikke tidligere lagret IM for journalpost ${journalpost.kanalReferanseId} for hendelse ${journalpost.hendelsesId}")
        }
        return dupe
    }
}
