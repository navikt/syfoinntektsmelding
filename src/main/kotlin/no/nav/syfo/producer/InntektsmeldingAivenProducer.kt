package no.nav.syfo.producer

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class InntektsmeldingAivenProducer(producerProperties: Map<String, Any>) {

    private val inntektsmeldingTopics = listOf("helsearbeidsgiver.privat-sykepenger-inntektsmelding")
    val objectMapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()

    private val kafkaproducer = KafkaProducer<String, String>(producerProperties)

    fun leggMottattInntektsmeldingPåTopics(inntektsmelding: Inntektsmelding) {
        inntektsmeldingTopics.forEach {
            leggMottattInntektsmeldingPåTopic(inntektsmelding, it)
        }
    }

    private fun leggMottattInntektsmeldingPåTopic(inntektsmelding: Inntektsmelding, topic: String) {
        kafkaproducer.send(
            ProducerRecord(
                topic,
                inntektsmelding.arbeidstakerFnr,
                serialiseringInntektsmelding(inntektsmelding)
            )
        )
    }

    fun serialiseringInntektsmelding(inntektsmelding: Inntektsmelding) =
        objectMapper.writeValueAsString(inntektsmelding)
}
