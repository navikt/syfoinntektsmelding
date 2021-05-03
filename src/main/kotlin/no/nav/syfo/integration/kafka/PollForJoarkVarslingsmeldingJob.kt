package no.nav.syfo.integration.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Duration

class PollForJoarkVarslingsmeldingJob(
        private val kafkaProvider: ManglendeInntektsmeldingMeldingProvider,
        private val service: VarslingService,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        waitTimeWhenEmptyQueue: Duration = Duration.ofSeconds(30)
) : RecurringJob(coroutineScope, waitTimeWhenEmptyQueue) {

    override fun doJob() {
        do {
            val wasEmpty = kafkaProvider
                    .getMessagesToProcess()
                    .onEach(service::handleMessage)
                    .isEmpty()

            if (!wasEmpty) {
                kafkaProvider.confirmProcessingDone()
            }
        } while (!wasEmpty)
    }
}
