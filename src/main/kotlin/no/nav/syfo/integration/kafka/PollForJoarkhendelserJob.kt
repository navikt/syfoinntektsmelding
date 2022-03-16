package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

fun mapInngaaendeJournalpostDTO(record: JournalfoeringHendelseRecord): InngaaendeJournalpostDTO {
    return InngaaendeJournalpostDTO(
        record.hendelsesId, record.versjon, record.hendelsesType, record.journalpostId,
        record.journalpostStatus, record.temaGammelt, record.temaNytt, record.mottaksKanal, record.kanalReferanseId, record.behandlingstema
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
                    log.info("JournalpostID ${it.journalpostId}")
                    // https://confluence.adeo.no/display/BOA/Tema https://confluence.adeo.no/display/BOA/Mottakskanal
                    val isSyketemaOgFraAltinnMidlertidig =
                        it.record.temaNytt == "SYK" &&
                            it.record.mottaksKanal == "ALTINN" &&
                            it.record.journalpostStatus == "M"

                    if (isSyketemaOgFraAltinnMidlertidig) {
                        log.info("Fant journalpost ${it.journalpostId} fra ALTINN for syk med status midlertidig.")
                        bakgrunnsjobbRepo.save(
                            Bakgrunnsjobb(
                                type = JoarkInntektsmeldingHendelseProsessor.JOB_TYPE,
                                kjoeretid = LocalDateTime.now(),
                                maksAntallForsoek = 10,
                                data = om.writeValueAsString(mapInngaaendeJournalpostDTO(it.record))
                            )
                        )
                    } else {
                        log.debug("Fant journalpost ${it.journalpostId} men ignorerer.")
                    }
                }
                .isEmpty()

            if (!wasEmpty) {
                kafkaProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }
}
