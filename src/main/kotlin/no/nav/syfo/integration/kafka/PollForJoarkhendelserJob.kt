package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
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
    private val om: ObjectMapper,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue.toMillis()) {

    private val log = LoggerFactory.getLogger(PollForJoarkhendelserJob::class.java)
    override fun doJob() {
        do {
            val wasEmpty = kafkaProvider
                .getMessagesToProcess()
                .onEach {
                    // https://confluence.adeo.no/display/BOA/Tema https://confluence.adeo.no/display/BOA/Mottakskanal
                    val isSyketemaOgFraAltinnMidlertidig =
                        it.temaNytt == "SYK" &&
                            it.mottaksKanal == "ALTINN" &&
                            it.journalpostStatus == "MOTTATT"

                    if (isSyketemaOgFraAltinnMidlertidig) {
                        log.info("Fant journalpost AR${it.journalpostId} fra ALTINN for syk med status midlertidig.")
                        bakgrunnsjobbRepo.save(
                            Bakgrunnsjobb(
                                type = JoarkInntektsmeldingHendelseProsessor.JOB_TYPE,
                                kjoeretid = LocalDateTime.now(),
                                maksAntallForsoek = 10,
                                data = om.writeValueAsString(it)
                            )
                        )
                    } else {
                        log.info("Fant journalpost AR${it.journalpostId} men ignorerer (tema: ${it.temaNytt}, kanal: ${it.mottaksKanal}, status: ${it.journalpostStatus})")
                    }
                }
                .isEmpty()

            if (!wasEmpty) {
                kafkaProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }
}
