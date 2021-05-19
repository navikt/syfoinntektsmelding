package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.helse.arbeidsgiver.utils.RecurringJob
import no.nav.syfo.kafkamottak.InngaaendeJournalpostDTO
import no.nav.syfo.prosesser.JoarkInntektsmeldingHendelseProsessor
import org.apache.commons.text.StringEscapeUtils
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class PollForJoarkhendelserJob(
    private val kafkaProvider: JoarkHendelseKafkaClient,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val om: ObjectMapper,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue.toMillis()) {

    override fun doJob() {
        do {
            lateinit var hendelse : InngaaendeJournalpostDTO
            val log = LoggerFactory.getLogger(PollForJoarkhendelserJob::class.java)
            val wasEmpty = kafkaProvider
                .getMessagesToProcess()
                .onEach {
                    try {
                        hendelse = om.readValue(it, InngaaendeJournalpostDTO::class.java)
                    } catch (e : JsonParseException) {
                        log.error("JsonParseError in  $it, removing non chars String ${StringEscapeUtils.escapeJson(it)}")
                    }

                    // https://confluence.adeo.no/display/BOA/Tema https://confluence.adeo.no/display/BOA/Mottakskanal
                    val isSyketemaOgFraAltinnMidlertidig =
                        hendelse.temaNytt == "SYK" &&
                            hendelse.mottaksKanal == "ALTINN" &&
                            hendelse.journalpostStatus == "M"

                    if (isSyketemaOgFraAltinnMidlertidig) {
                        bakgrunnsjobbRepo.save(
                            Bakgrunnsjobb(
                                type = JoarkInntektsmeldingHendelseProsessor.JOB_TYPE,
                                kjoeretid = LocalDateTime.now(),
                                maksAntallForsoek = 10,
                                data = om.writeValueAsString(
                                    JoarkInntektsmeldingHendelseProsessor.JobbData(
                                        UUID.randomUUID(),
                                        om.writeValueAsString(hendelse)
                                    )
                                )
                            )
                        )
                    }
                }
                .isEmpty()

            if (!wasEmpty) {
                kafkaProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }
}
