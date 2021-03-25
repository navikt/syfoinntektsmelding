package no.nav.syfo.integration.kafka

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

interface ConsumerFactory<K, V> {
    fun createConsumer(props : Map<String, Any>) : Consumer<K, V>
}

class UtsattOppgaveConsumerFactory : ConsumerFactory<String, Any> {
    override fun createConsumer(props: Map<String, Any>) = KafkaConsumer<String, Any>(
        props + mapOf(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to UtsattOppgaveDTODeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v1"
        ))
}


class JoarchhendlseConsumerFactory(val kafkaSchemaRegistryUrl: String) : ConsumerFactory<Nokkel, Beskjed> {
    override fun createConsumer(props: Map<String, Any>) = KafkaConsumer<Nokkel, Beskjed>(
        props + mapOf(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to GenericAvroDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "none",
            ConsumerConfig.GROUP_ID_CONFIG to "syfoinntektsmelding-v2",
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaSchemaRegistryUrl
        )
    )
}

class UtsattOppgaveDTODeserializer : Deserializer<UtsattOppgaveDTO> {
    val om = ObjectMapper()
    override fun deserialize(topic: String, data: ByteArray): UtsattOppgaveDTO {
        return om.readValue(data)
    }
}
