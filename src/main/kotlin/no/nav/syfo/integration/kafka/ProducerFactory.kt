package no.nav.syfo.integration.kafka


import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer

interface ProducerFactory<K, V> {
    fun createProducer(props : Map<String, Any>) : Producer<K, V>
}

class StringKafkaProducerFactory : ProducerFactory<String, String> {
    override fun createProducer(props: Map<String, Any>) = KafkaProducer(
        props + mapOf(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName
        ),
        StringSerializer(),
        StringSerializer())
}



class BeskjedProducerFactory(private val kafkaSchemaRegistryUrl: String) : ProducerFactory<Nokkel, Beskjed> {
    override fun createProducer(props: Map<String, Any>) = KafkaProducer<Nokkel, Beskjed>(
        props + mapOf(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaSchemaRegistryUrl
        )
    )
}

