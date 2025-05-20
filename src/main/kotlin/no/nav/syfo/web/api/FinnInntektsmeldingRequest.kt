@file:UseSerializers(LocalDateSerializer::class)

package no.nav.syfo.web.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class FinnInntektsmeldingRequest(
    val fnr: String,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)
