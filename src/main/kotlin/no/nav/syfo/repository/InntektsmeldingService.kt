package no.nav.syfo.repository

import log
import lombok.extern.slf4j.Slf4j
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingEntitet
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

@Service
@Slf4j
class InntektsmeldingService(
    private val repository: InntektsmeldingRepository,
    @Value("\${inntektsmelding.lagringstid.maneder:3}") val lagringstidMåneder: Int
) {

    val objectMapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()
    val log = log()


    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<Inntektsmelding> {
        val liste = repository.findByAktorId(aktoerId)
        return liste.map { InntektsmeldingMeta -> toInntektsmelding(InntektsmeldingMeta) }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    @org.springframework.transaction.annotation.Transactional("transactionManager")
    fun lagreBehandling(inntektsmelding: Inntektsmelding, aktorid: String, saksId: String, arkivReferanse: String): InntektsmeldingEntitet {
        val dto = toInntektsmeldingEntitet(inntektsmelding)
        dto.aktorId = aktorid
        dto.sakId = saksId
        dto.data = mapString(inntektsmelding)
        return repository.saveAndFlush(dto)
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

    fun mapString(inntektsmelding: Inntektsmelding): String {
        val im = inntektsmelding.copy()
        im.fnr = "" // La stå! Ikke lagre fødselsnummer
        return objectMapper.writeValueAsString(im)
    }
}
