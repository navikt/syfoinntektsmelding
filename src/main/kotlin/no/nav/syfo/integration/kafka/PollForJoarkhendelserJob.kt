package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

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
                    val hendelse = om.readValue(it, InngaaendeJournalpostDTO::class.java)
                    // https://confluence.adeo.no/display/BOA/Tema https://confluence.adeo.no/display/BOA/Mottakskanal
                    val isSyketemaOgFraAltinnMidlertidig =
                        hendelse.temaNytt == "SYK" &&
                            hendelse.mottaksKanal == "ALTINN" &&
                            hendelse.journalpostStatus == "M"

                    if (isSyketemaOgFraAltinnMidlertidig) {
                        log.info("Fant journalpost ${hendelse.journalpostId} fra ALTINN for syk med status midlertidig.")
                        bakgrunnsjobbRepo.save(
                            Bakgrunnsjobb(
                                type = JoarkInntektsmeldingHendelseProsessor.JOB_TYPE,
                                kjoeretid = LocalDateTime.now(),
                                maksAntallForsoek = 10,
                                data = it
                            )
                        )
                    } else {
                        log.debug("Fant journalpost ${hendelse.journalpostId} men ignorerer.")
                    }
                }
                .isEmpty()

            if (!wasEmpty) {
                kafkaProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }
}
