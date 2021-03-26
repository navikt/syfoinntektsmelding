package no.nav.syfo.repository

import com.fasterxml.jackson.databind.ObjectMapper
import log
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helse.arbeidsgiver.bakgrunnsjobb.BakgrunnsjobbRepository
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingEntitet
import no.nav.syfo.prosesser.FjernInnteksmeldingByBehandletProcessor
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class InntektsmeldingService(
    private val repository: InntektsmeldingRepository,
    private val bakgrunnsjobbRepo: BakgrunnsjobbRepository,
    private val objectMapper : ObjectMapper
) {
    val log = log()

    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<Inntektsmelding> {
        val liste = repository.findByAktorId(aktoerId)
        return liste.map { InntektsmeldingMeta -> toInntektsmelding(InntektsmeldingMeta) }
    }

    fun lagreBehandling(inntektsmelding: Inntektsmelding, aktorid: String, saksId: String, arkivReferanse: String): InntektsmeldingEntitet {
        val dto = toInntektsmeldingEntitet(inntektsmelding)
        dto.aktorId = aktorid
        dto.sakId = saksId
        dto.data = inntektsmelding.asJsonString(objectMapper)
        return repository.lagreInnteksmelding(dto)
    }

    fun slettInntektsmeldingerEldreEnnKonfigurertMåneder() {
        bakgrunnsjobbRepo.save(
            Bakgrunnsjobb(
                type = FjernInnteksmeldingByBehandletProcessor.JOB_TYPE,
                kjoeretid = LocalDate.now().plusDays(1).atStartOfDay().plusHours(4),
                maksAntallForsoek = 10,
                data = objectMapper.writeValueAsString(FjernInnteksmeldingByBehandletProcessor.JobbData(UUID.randomUUID()))
            )
        )
    }
}

fun Inntektsmelding.asJsonString(objectMapper: ObjectMapper): String {
    val im = this.copy(fnr = "") // La stå! Ikke lagre fødselsnummer
    return objectMapper.writeValueAsString(im)
}
