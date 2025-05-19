@file:UseSerializers(LocalDateTimeSerializer::class)

package no.nav.syfo.web.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class FinnInntektsmeldingRequest(
    val fnr: String,
    val fom: LocalDateTime? = null,
    val tom: LocalDateTime? = null,
)
