package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.kafka.common.serialization.Deserializer

class JoarkHendelseDeserializer : Deserializer<JoarkHendelse> {
    val om = ObjectMapper()
    override fun deserialize(topic: String, data: ByteArray): JoarkHendelse {
        return om.readValue(data)
    }
}
