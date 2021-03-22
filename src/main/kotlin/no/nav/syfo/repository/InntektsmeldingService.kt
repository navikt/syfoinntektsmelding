package no.nav.syfo.repository

import com.fasterxml.jackson.databind.ObjectMapper
import log
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingEntitet
import java.time.LocalDate
import javax.transaction.Transactional


class InntektsmeldingService(
    private val repository: InntektsmeldingRepository,
    val lagringstidMåneder: Int,
    private val objectMapper: ObjectMapper
) {
    val log = log()

    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<Inntektsmelding> {
        val liste = repository.findByAktorId(aktoerId)
        return liste.map { InntektsmeldingMeta -> toInntektsmelding(InntektsmeldingMeta) }
    }

    fun lagreBehandling(inntektsmelding: Inntektsmelding, aktorid: String, saksId: String, arkivReferanse: String): InntektsmeldingEntitet? {
        val dto = toInntektsmeldingEntitet(inntektsmelding)
        dto.aktorId = aktorid
        dto.sakId = saksId
        dto.data = inntektsmelding.asJsonString()
        return repository.lagreInnteksmelding(dto)
    }

    // Sekund, Minutt, Time, Dag, Måned, Ukedag
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional(Transactional.TxType.REQUIRED)
    @org.springframework.transaction.annotation.Transactional("transactionManager")
    fun slettInntektsmeldingerEldreEnnKonfigurertMåneder() {
        val konfigurertAntallMånederSiden = LocalDate.now().minusMonths(lagringstidMåneder.toLong()).atStartOfDay()
        log.info("Sletter alle inntektsmeldinger før $konfigurertAntallMånederSiden")
        val antallSlettet = repository.deleteByBehandletBefore(konfigurertAntallMånederSiden)
        log.info("Slettet $antallSlettet inntektsmeldinger")
    }
}

fun Inntektsmelding.asJsonString(): String {
    val im = this.copy(fnr = "") // La stå! Ikke lagre fødselsnummer
    return objectMapper.writeValueAsString(im)
}
