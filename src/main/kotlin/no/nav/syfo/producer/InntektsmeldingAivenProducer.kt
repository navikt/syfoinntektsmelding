package no.nav.syfo.producer

import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

private const val IM_VEDTAKSLOESNING_TOPIC = "helsearbeidsgiver.privat-sykepenger-inntektsmelding"
private const val IM_BRUKER_TOPIC = "helsearbeidsgiver.inntektsmelding-bruker"

class InntektsmeldingAivenProducer(
    producerProperties: Map<String, Any>,
) {
    private val sikkerlogger = sikkerLogger()
    private val logger = logger()
    val objectMapper = JacksonJsonConfig.opprettObjectMapper()
    private val kafkaproducer = KafkaProducer<String, String>(producerProperties)

    fun sendTilTopicForVedtaksloesning(inntektsmelding: Inntektsmelding) {
        leggMottattInntektsmeldingPåTopic(inntektsmelding, IM_VEDTAKSLOESNING_TOPIC)
    }

    fun sendTilTopicForBruker(inntektsmelding: Inntektsmelding) {
        leggMottattInntektsmeldingPåTopic(inntektsmelding, IM_BRUKER_TOPIC)
    }

    private fun leggMottattInntektsmeldingPåTopic(
        inntektsmelding: Inntektsmelding,
        topic: String,
    ) {
        val serialisertIM = serialiseringInntektsmelding(inntektsmelding)
        sikkerlogger.info("Publiserer på $topic: $serialisertIM")
        logger.info("Publiserer på $topic inntektsmelding med id ${inntektsmelding.inntektsmeldingId}")
        kafkaproducer.send(
            ProducerRecord(
                topic,
                inntektsmelding.arbeidstakerFnr,
                serialisertIM,
            ),
        )
    }

    fun serialiseringInntektsmelding(inntektsmelding: Inntektsmelding): String = objectMapper.writeValueAsString(inntektsmelding)
}
