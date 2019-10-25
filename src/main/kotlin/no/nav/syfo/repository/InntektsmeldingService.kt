package no.nav.syfo.repository

import lombok.extern.slf4j.Slf4j
import no.nav.syfo.domain.inntektsmelding.Inntektsmelding
import no.nav.syfo.dto.InntektsmeldingDto
import no.nav.syfo.mapping.toInntektsmelding
import no.nav.syfo.mapping.toInntektsmeldingDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Slf4j
class InntektsmeldingService {

    @Autowired
    private lateinit var repository: InntektsmeldingRepository

    fun opprett(inntektsmelding: Inntektsmelding): String {
        val dto = toInntektsmeldingDTO(inntektsmelding)
        println("Oppretter inntektsmelding ${dto.uuid}")
        return repository.save(dto).uuid!!
    }

    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<Inntektsmelding> {
        val liste = repository.findByAktorId(aktoerId)
        return liste.map{ InntektsmeldingMeta -> toInntektsmelding(InntektsmeldingMeta) }
    }

    fun lagreBehandling(inntektsmelding: Inntektsmelding, aktorid: String, saksId: String, arkivReferanse: String): InntektsmeldingDto {
        val dto = toInntektsmeldingDTO(inntektsmelding)
        dto.aktorId = aktorid
        dto.sakId = saksId
        println("Lagrer inntektsmelding ${dto.uuid}")
        return repository.save(dto)
    }

}


//@Service
//@Slf4j
//@Transactional(transactionManager = "datasourceTransactionManager")
//@Repository
//class InntektsmeldingDAO(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
//
//    fun opprett(inntektsmeldingMeta: InntektsmeldingMeta): String {
//        val uuid = UUID.randomUUID().toString()
//        namedParameterJdbcTemplate.update(
//            "INSERT INTO INNTEKTSMELDING(INNTEKTSMELDING_UUID, AKTOR_ID, SAK_ID, ORGNUMMER, JOURNALPOST_ID, BEHANDLET, ARBEIDSGIVER_PRIVAT) " + "VALUES(:uuid, :aktorid, :sakid, :orgnummer, :journalpostId, :behandlet, :arbeidsgiverPrivat)",
//
//            MapSqlParameterSource()
//                .addValue("uuid", uuid)
//                .addValue("aktorid", inntektsmeldingMeta.aktorId)
//                .addValue("orgnummer", inntektsmeldingMeta.orgnummer)
//                .addValue("sakid", inntektsmeldingMeta.sakId)
//                .addValue("journalpostId", inntektsmeldingMeta.journalpostId)
//                .addValue("behandlet", inntektsmeldingMeta.behandlet)
//                .addValue("arbeidsgiverPrivat", inntektsmeldingMeta.arbeidsgiverPrivat)
//        )
//
//        inntektsmeldingMeta.arbeidsgiverperioder.forEach { (fom, tom) ->
//            namedParameterJdbcTemplate.update(
//                "INSERT INTO ARBEIDSGIVERPERIODE(PERIODE_UUID, INNTEKTSMELDING_UUID, FOM, TOM) VALUES(:periodeUuid, :inntektsmeldingUuid, :fom, :tom)",
//                MapSqlParameterSource()
//                    .addValue("periodeUuid", UUID.randomUUID().toString())
//                    .addValue("inntektsmeldingUuid", uuid)
//                    .addValue("fom", fom)
//                    .addValue("tom", tom)
//            )
//        }
//        return uuid
//    }
//
//    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<InntektsmeldingMeta> {
//        return namedParameterJdbcTemplate.query(
//            "SELECT * FROM INNTEKTSMELDING WHERE (AKTOR_ID = :aktorid)",
//            MapSqlParameterSource()
//                .addValue("aktorid", aktoerId)
//        ) { rs, _ ->
//            val inntektsmeldingUuid = rs.getString("INNTEKTSMELDING_UUID")
//
//            InntektsmeldingMeta(
//                uuid = inntektsmeldingUuid,
//                aktorId = rs.getString("AKTOR_ID"),
//                orgnummer = rs.getString("ORGNUMMER"),
//                arbeidsgiverPrivat = rs.getString("ARBEIDSGIVER_PRIVAT"),
//                arbeidsgiverperioder = hentPerioder(inntektsmeldingUuid),
//                sakId = rs.getString("SAK_ID"),
//                journalpostId = rs.getString("JOURNALPOST_ID"),
//                behandlet = rs.getTimestamp("BEHANDLET")?.toLocalDateTime()
//            )
//        }
//    }
//
//    private fun hentPerioder(inntektsmeldingUuid: String): List<Periode> {
//        return namedParameterJdbcTemplate.query(
//            "SELECT * FROM ARBEIDSGIVERPERIODE WHERE (INNTEKTSMELDING_UUID = :inntektsmeldingUUID)",
//            MapSqlParameterSource()
//                .addValue("inntektsmeldingUUID", inntektsmeldingUuid)
//        ) { rs, _ -> Periode(rs.getDate("FOM").toLocalDate(), rs.getDate("TOM").toLocalDate()) }
//    }
//}
