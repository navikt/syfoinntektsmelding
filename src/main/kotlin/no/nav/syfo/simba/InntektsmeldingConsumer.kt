package no.nav.syfo.simba

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class InntektsmeldingConsumer(props: Map<String,Any>, topicName: String) {

    private val consumer = KafkaConsumer<String, String>(props)
    private var ready = false
    private var error = false
    private val log = LoggerFactory.getLogger(InntektsmeldingConsumer::class.java)

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
                it
                    .poll(Duration.ofMillis(1000))
                    .forEach { record ->
                        try {
                            behandle(record.value())
                            it.commitSync()
                        } catch (e: Throwable) {
                            log.error("Klarte ikke behandle hendelse. Stopper lytting!", e)
                            setIsError(true)
                        }
                    }
            }
        }
    }

    fun behandle(value: String) {
        //
        log.info("Fikk inntektsmelding fra simba: $value")
    }

}
