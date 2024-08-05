package no.nav.syfo.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingEntitet
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingEntitet
import no.nav.syfo.repository.InntektsmeldingRepository
import org.slf4j.Logger

class InntektsmeldingService(
    private val repository: InntektsmeldingRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = this.logger()

    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<Inntektsmelding> {
        return repository.findByAktorId(aktoerId).map { toInntektsmelding(it, objectMapper) }
    }

    fun findByJournalpost(journalpostId: String): InntektsmeldingEntitet? {
        return repository.findByJournalpost(journalpostId)
    }

    fun isDuplicate(inntektsmelding: Inntektsmelding): Boolean {
        if (inntektsmelding.aktorId == null) {
            return false
        }
        return isDuplicateWithLatest(logger, inntektsmelding, finnBehandledeInntektsmeldinger(inntektsmelding.aktorId!!))
    }

    fun lagreBehandling(
        inntektsmelding: Inntektsmelding,
        aktorid: String
    ): InntektsmeldingEntitet {
        val dto = toInntektsmeldingEntitet(inntektsmelding)
        dto.aktorId = aktorid
        dto.data = inntektsmelding.asJsonString(objectMapper)
        return repository.lagreInnteksmelding(dto)
    }
}

fun Inntektsmelding.asJsonString(objectMapper: ObjectMapper): String {
    val im = this.copy(fnr = "") // La stå! Ikke lagre fødselsnummer
    return objectMapper.writeValueAsString(im)
}

fun isDuplicateWithLatest(logger: Logger, inntektsmelding: Inntektsmelding, list: List<Inntektsmelding>): Boolean {
    if (list.isEmpty()) {
        return false
    }
    val nyesteInntektsmelding = list.sortedBy { it.mottattDato }.last()
    val duplikatLatest = inntektsmelding.isDuplicate(nyesteInntektsmelding)
    val duplikatExclusive = inntektsmelding.isDuplicateExclusiveArsakInnsending(nyesteInntektsmelding) // TODO: Fjerne denne sjekken?
    logger.info("Likhetssjekk: Er duplikat ekslusive ÅrsakInnsending? ${!duplikatLatest && duplikatExclusive} Journalpost: ${inntektsmelding.journalpostId} ")
    return duplikatLatest
}
