package no.nav.syfo.consumer

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private const val TOPIC = "placeholder-topic"

@Component
class OppgaveListener {

    @KafkaListener(id = "oppgaver", topics = [TOPIC])
    fun lyttPåOppgaveKø(melding: String) {

    }

}
