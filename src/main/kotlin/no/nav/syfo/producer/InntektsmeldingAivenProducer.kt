package no.nav.syfo.producer

import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class InntektsmeldingAivenProducer(producerProperties: Map<String, Any>) {

    private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    private val inntektsmeldingTopics = listOf("helsearbeidsgiver.privat-sykepenger-inntektsmelding")
    val objectMapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()

    private val kafkaproducer = KafkaProducer<String, String>(producerProperties)

    fun leggMottattInntektsmeldingPåTopics(inntektsmelding: Inntektsmelding) {
        inntektsmeldingTopics.forEach {
            leggMottattInntektsmeldingPåTopic(inntektsmelding, it)
        }
    }

    private fun leggMottattInntektsmeldingPåTopic(inntektsmelding: Inntektsmelding, topic: String) {
        val serialisertIM = serialiseringInntektsmelding(inntektsmelding)
        sikkerlogger.info("Publiserer på $topic: $serialisertIM") // Midlertidig logging
        kafkaproducer.send(
            ProducerRecord(
                topic,
                inntektsmelding.arbeidstakerFnr,
                serialisertIM
            )
        )
    }

    fun serialiseringInntektsmelding(inntektsmelding: Inntektsmelding) =
        objectMapper.writeValueAsString(inntektsmelding)
}
