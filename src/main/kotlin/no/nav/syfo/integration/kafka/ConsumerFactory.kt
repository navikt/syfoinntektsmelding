package no.nav.syfo.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.utsattoppgave.UtsattOppgaveDTO
import org.apache.kafka.common.serialization.Deserializer


class UtsattOppgaveDTODeserializer : Deserializer<UtsattOppgaveDTO> {
    val om = ObjectMapper()
    override fun deserialize(topic: String, data: ByteArray): UtsattOppgaveDTO {
        return om.readValue(data)
    }
}
