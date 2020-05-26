package no.nav.syfo.consumer.kafka

import java.time.LocalDateTime
import java.util.UUID

data class JoarkEventDTO(
    val dokumentId: UUID,
    val timeout: LocalDateTime? = null
)
