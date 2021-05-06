package no.nav.syfo.producer

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.syfo.util.Metrikk
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*


class InntektsmeldingProducer(producerProperties : Properties,
                              private val metrikk: Metrikk
) {
    private val inntektsmeldingTopics = listOf("privat-sykepenger-inntektsmelding")
    val objectMapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()

    private val kafkaproducer = KafkaProducer<String, String>(producerProperties)

    fun leggMottattInntektsmeldingPåTopics(inntektsmelding: Inntektsmelding) {
        inntektsmeldingTopics.forEach {
            leggMottattInntektsmeldingPåTopic(inntektsmelding, it)
        }
        metrikk.tellInntektsmeldingLagtPåTopic()
    }

    private fun leggMottattInntektsmeldingPåTopic(inntektsmelding: Inntektsmelding, topic: String) {
        kafkaproducer.send(ProducerRecord(topic, inntektsmelding.arbeidstakerFnr, serialiseringInntektsmelding(inntektsmelding)))
    }

    fun serialiseringInntektsmelding(inntektsmelding: Inntektsmelding): String =
        objectMapper.writeValueAsString(inntektsmelding)
}

