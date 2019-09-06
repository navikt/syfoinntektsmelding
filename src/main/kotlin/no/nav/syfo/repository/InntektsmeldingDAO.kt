package no.nav.syfo.repository

import lombok.extern.slf4j.Slf4j
import no.nav.syfo.domain.InntektsmeldingMeta
import no.nav.syfo.domain.Periode
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Slf4j
@Transactional(transactionManager = "datasourceTransactionManager")
@Repository
class InntektsmeldingDAO(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun opprett(inntektsmeldingMeta: InntektsmeldingMeta): String {
        val uuid = UUID.randomUUID().toString()
        namedParameterJdbcTemplate.update(
            "INSERT INTO INNTEKTSMELDING(INNTEKTSMELDING_UUID, AKTOR_ID, SAK_ID, ORGNUMMER, JOURNALPOST_ID, BEHANDLET, ARBEIDSGIVER_PRIVAT) " + "VALUES(:uuid, :aktorid, :sakid, :orgnummer, :journalpostId, :behandlet, :arbeidsgiverPrivat)",

            MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("aktorid", inntektsmeldingMeta.aktorId)
                .addValue("orgnummer", inntektsmeldingMeta.orgnummer)
                .addValue("sakid", inntektsmeldingMeta.sakId)
                .addValue("journalpostId", inntektsmeldingMeta.journalpostId)
                .addValue("behandlet", inntektsmeldingMeta.behandlet)
                .addValue("arbeidsgiverPrivat", inntektsmeldingMeta.arbeidsgiverPrivat)
        )

        inntektsmeldingMeta.arbeidsgiverperioder.forEach { (fom, tom) ->
            namedParameterJdbcTemplate.update(
                "INSERT INTO ARBEIDSGIVERPERIODE(PERIODE_UUID, INNTEKTSMELDING_UUID, FOM, TOM) VALUES(:periodeUuid, :inntektsmeldingUuid, :fom, :tom)",
                MapSqlParameterSource()
                    .addValue("periodeUuid", UUID.randomUUID().toString())
                    .addValue("inntektsmeldingUuid", uuid)
                    .addValue("fom", fom)
                    .addValue("tom", tom)
            )
        }
        return uuid
    }

    fun finnBehandledeInntektsmeldinger(aktoerId: String): List<InntektsmeldingMeta> {
        return namedParameterJdbcTemplate.query(
            "SELECT * FROM INNTEKTSMELDING WHERE (AKTOR_ID = :aktorid)",
            MapSqlParameterSource()
                .addValue("aktorid", aktoerId)
        ) { rs, _ ->
            val inntektsmeldingUuid = rs.getString("INNTEKTSMELDING_UUID")

            InntektsmeldingMeta(
                uuid = inntektsmeldingUuid,
                aktorId = rs.getString("AKTOR_ID"),
                orgnummer = rs.getString("ORGNUMMER"),
                arbeidsgiverPrivat = rs.getString("ARBEIDSGIVER_PRIVAT"),
                arbeidsgiverperioder = hentPerioder(inntektsmeldingUuid),
                sakId = rs.getString("SAK_ID"),
                journalpostId = rs.getString("JOURNALPOST_ID"),
                behandlet = rs.getTimestamp("BEHANDLET")?.toLocalDateTime()
            )
        }
    }

    private fun hentPerioder(inntektsmeldingUuid: String): List<Periode> {
        return namedParameterJdbcTemplate.query(
            "SELECT * FROM ARBEIDSGIVERPERIODE WHERE (INNTEKTSMELDING_UUID = :inntektsmeldingUUID)",
            MapSqlParameterSource()
                .addValue("inntektsmeldingUUID", inntektsmeldingUuid)
        ) { rs, _ -> Periode(rs.getDate("FOM").toLocalDate(), rs.getDate("TOM").toLocalDate()) }
    }
}
