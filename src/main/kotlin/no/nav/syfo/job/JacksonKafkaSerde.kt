package no.nav.syfo.job

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import no.nav.syfo.utsattoppgave.objectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

class UtsattOppgaveDeserializer : Deserializer<JsonNode?> {
    override fun deserialize(topic: String?, data: ByteArray) = try {
        objectMapper.readTree(data)
    } catch (exception: JsonParseException) {
        null
    }
}


class UtsattOppgaveSerializer<T> : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray = objectMapper.writeValueAsBytes(data)
}
