package no.nav.syfo.utsattoppgave

import log
import no.nav.syfo.behandling.OppgaveException
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.springframework.kafka.listener.ContainerAwareErrorHandler
import org.springframework.kafka.listener.ContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component

private val STOPPING_ERROR_HANDLER = ContainerStoppingErrorHandler()

@Component
class KafkaErrorHandler : ContainerAwareErrorHandler {
    val log = log()

    val retryableExceptionTypes = setOf(
        TopicAuthorizationException::class.java,
        OppgaveException::class.java
    )

    override fun handle(
            thrownException: Exception,
            records: List<ConsumerRecord<*, *>>?,
            consumer: Consumer<*, *>?,
            container: MessageListenerContainer
    ) {
        log.error("Feil i listener:", thrownException)

        if (retryableExceptionTypes.any { exceptionIsClass(thrownException, it) }) {
            log.error("Kafka infrastrukturfeil. ${thrownException::class.java.name} ved lesing av topic")

            Thread {
                try {
                    Thread.sleep(10000)
                    log.info("Starter ny kafka-consumer")
                    container.start()
                } catch (e: Exception) {
                    // Denne kunne ha med fordel ha flippa applikasjons selftest
                    log.error("Noe gikk galt ved oppstart av kafka-consumer", e)
                    throw thrownException
                }
            }.start()
            log.error("Restarter kafka-consumeren")
        }
        log.error("Uventet feil i kafka-consumeren - stopper lytteren")
        STOPPING_ERROR_HANDLER.handle(thrownException, records, consumer, container)
    }

    private fun exceptionIsClass(throwable: Throwable?, klazz: Class<*>): Boolean {
        var t = throwable
        var maxdepth = 10
        while (maxdepth-- > 0 && t != null && !klazz.isInstance(t)) {
            t = t.cause
        }

        return klazz.isInstance(t)
    }
}
