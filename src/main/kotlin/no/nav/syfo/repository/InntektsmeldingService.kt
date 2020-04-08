package no.nav.syfo.repository

import lombok.extern.slf4j.Slf4j
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingEntitet
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

@Service
@Slf4j
class InntektsmeldingService(
    private val repository: InntektsmeldingRepository
) {

    val objectMapper = JacksonJsonConfig.objectMapperFactory.opprettObjectMapper()


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

    @Scheduled(cron = "0 4 * * *")
    @Transactional(Transactional.TxType.REQUIRED)
    @org.springframework.transaction.annotation.Transactional("transactionManager")
    fun slettInntektsmeldingerEldreEnnTreMåneder() {
        val treMånederSiden = LocalDate.now().minusMonths(3).atStartOfDay()
        repository.deleteByBehandletBefore(treMånederSiden)
    }

    fun mapString(inntektsmelding: Inntektsmelding): String {
        val im = inntektsmelding.copy()
        im.fnr = "" // La stå! Ikke lagre fødselsnummer
        return objectMapper.writeValueAsString(im)
    }
}
