package no.nav.syfo

import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.io.File
import java.util.Properties
import java.util.UUID

private const val INNTEKTSMELDING_ID = "%%%INNTEKTSMELDING_ID%%%"
private const val FORESPOERSEL_ID = "%%%FORESPOERSEL_ID%%%"
private const val SYKMELDT_FNR = "%%%SYKMELDT%%%"
private const val ORGNUMMER = "%%%ORGNR%%%"
const val DEFAULT_FNR = "07025032327"
const val DEFAULT_ORG = "810007842"

fun main() {
    genererKafkaMeldinger()
}

fun genererKafkaMeldinger() {
    val journalfoertInntektsmelding = buildJournalfoertInntektsmelding()

    val imTopic = "inntektsmelding"

    val imRecord = ProducerRecord(imTopic, "key", journalfoertInntektsmelding)

    Producer.sendMelding(imRecord)

    Producer.kafkaProducer.close()
}

object Producer {
    val props =
        Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092")
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.CLIENT_ID_CONFIG, "spinosaurus")
        }
    val kafkaProducer = KafkaProducer<String, String>(props)

    fun sendMelding(record: ProducerRecord<String, String>) {
        kafkaProducer.send(record) { metadata, exception ->
            if (exception != null) {
                println("Error sending message: ${exception.message}")
            } else {
                println("Message sent to topic ${metadata.topic()} partition ${metadata.partition()} with offset ${metadata.offset()}")
            }
        }
    }
}

fun buildJournalfoertInntektsmelding(
    inntektsmeldingId: UUID = UUID.randomUUID(),
    forespoerselId: UUID = UUID.randomUUID(),
    sykemeldtFnr: Fnr = Fnr(DEFAULT_FNR),
    orgNr: Orgnr = Orgnr(DEFAULT_ORG),
): String {
    val filePath = "journalfoertInntektsmelding.json"
    return readJsonFromResources(filePath)
        .replace(INNTEKTSMELDING_ID, inntektsmeldingId.toString())
        .replace(FORESPOERSEL_ID, forespoerselId.toString())
        .replace(SYKMELDT_FNR, sykemeldtFnr.verdi)
        .replace(ORGNUMMER, orgNr.verdi)
}

fun readJsonFromResources(fileName: String): String {
    val resource = KafkaProducer::class.java.getResource("/$fileName")
    return File(resource!!.toURI()).readText(Charsets.UTF_8)
}
