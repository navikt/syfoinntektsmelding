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

    private val blacklist = listOf(
        "627351414",
        "627350732",
        "627336854",
        "627336640",
        "627336534",
        "627334462",
        "627331640",
        "627268840",
        "627265545",
        "627265342",
        "627255895",
        "627252298",
        "627243972",
        "627233588",
        "627232341",
        "627211515",
        "627209154",
        "627208242",
        "627201868",
        "627201340",
        "627169800",
        "627168910",
        "627163007",
        "627157276",
        "627155198",
        "627141609",
        "627141091",
        "627137999",
        "627137948",
        "627137066",
        "627134871",
        "627131543",
        "627131374",
        "627121722",
        "627119720",
        "627112494",
        "627111765",
        "627110410",
        "627110130",
        "627107088",
        "627097157",
        "627071004",
        "627069534",
        "627055952",
        "627054170",
        "627041638",
        "627034718",
        "627034128",
        "627033382",
        "627031166",
        "627020102",
        "626996034",
        "626995826",
        "626992875",
        "626988916",
        "626988091",
        "626987826",
        "626982096",
        "626956940",
        "626925188",
        "626924381",
        "626913844",
        "626908114",
        "626899524",
        "626899355",
        "626893467",
        "626886675",
        "626885576",
        "626883681",
        "626881609",
        "626880989",
        "626866047",
        "626863231",
        "626847550",
        "626839907",
        "626835385",
        "626831412",
        "626830718",
        "626817282",
        "626804105",
        "626799289",
        "626798732",
        "626659687",
        "626656955",
        "626513376",
        "626502715",
        "626495784",
        "626437089",
        "626388467",
        "626240230",
        "626223754",
        "626170290",
        "626130488",
        "618774620",
    )
    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<Inntektsmelding> {
        return repository.findByAktorId(aktoerId).map { toInntektsmelding(it, objectMapper) }
    }

    fun findByJournalpost(journalpostId: String): InntektsmeldingEntitet? {
        if (journalpostId in blacklist) {
            logger.info("Ignorer $journalpostId!")
            return null
        }
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
    val sortedList = list.sortedBy { it.innsendingstidspunkt }.last()
    val duplikatLatest = inntektsmelding.isDuplicate(sortedList)
    val duplikatExclusive = inntektsmelding.isDuplicateExclusiveArsakInnsending(sortedList)
    logger.info("Likhetssjekk: Er duplikat ekslusive ÅrsakInnsending? ${!duplikatLatest && duplikatExclusive} Journalpost: ${inntektsmelding.journalpostId} ")
    return duplikatLatest
}
