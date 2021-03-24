package no.nav.syfo.utsattoppgave

import log
import no.nav.syfo.web.selftest.SimpleSelfTestState

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.ContainerAwareErrorHandler
import org.springframework.kafka.listener.ContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer


private val STOPPING_ERROR_HANDLER = ContainerStoppingErrorHandler()

class InfiniteRetryKafkaErrorHandler(private val selfTestState: SimpleSelfTestState) : ContainerAwareErrorHandler {
    val log = log()

    override fun handle(
            thrownException: Exception,
            records: List<ConsumerRecord<*, *>>?,
            consumer: Consumer<*, *>?,
            container: MessageListenerContainer
    ) {
        log.error("Feil i listener:", thrownException)

        Thread {
            try {
                Thread.sleep(10000)
                log.info("Starter ny kafka-consumer")
                container.start()
            } catch (e: Exception) {
                log.error("Noe gikk galt ved oppstart av kafka-consumer", e)
                selfTestState.IS_ALIVE = false
                throw thrownException
            }
        }.start()

        log.error("Restarter kafka-consumeren")
        log.error("Uventet feil i kafka-consumeren - stopper lytteren")
        STOPPING_ERROR_HANDLER.handle(thrownException, records, consumer, container)
    }
}
