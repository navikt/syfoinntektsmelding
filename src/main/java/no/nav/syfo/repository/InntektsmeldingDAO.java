package no.nav.syfo.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@Repository
public class InntektsmeldingDAO {

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public InntektsmeldingDAO(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public void opprett(InntektsmeldingMeta inntektsmeldingMeta) {
        namedParameterJdbcTemplate.update(
                "INSERT INTO INNTEKTSMELDING(INNTEKTSMELDING_UUID, AKTOR_ID, ARBEIDSGIVERPERIODE_FOM, ARBEIDSGIVERPERIODE_TOM, SAK_ID, ORGNUMMER, JOURNALPOST_ID, BEHANDLET) " +
                        "VALUES(:uuid, :aktorid, :arbeidsgiverperiodeFom, :arbeidsgiverperiodeTom, :sakid, :orgnummer, :journalpostId, :behandlet)",

                new MapSqlParameterSource()
                        .addValue("uuid", UUID.randomUUID().toString())
                        .addValue("aktorid", inntektsmeldingMeta.getAktorId())
                        .addValue("orgnummer", inntektsmeldingMeta.getOrgnummer())
                        .addValue("arbeidsgiverperiodeFom", inntektsmeldingMeta.getArbeidsgiverperiodeFom())
                        .addValue("arbeidsgiverperiodeTom", inntektsmeldingMeta.getArbeidsgiverperiodeTom())
                        .addValue("sakid", inntektsmeldingMeta.getSakId())
                        .addValue("journalpostId", inntektsmeldingMeta.getJournalpostId())
                        .addValue("behandlet", inntektsmeldingMeta.getBehandlet())
        );
    }

    public List<InntektsmeldingMeta> finnBehandledeInntektsmeldinger(String aktoerId, String orgnummer) {
        return namedParameterJdbcTemplate.query("SELECT * FROM INNTEKTSMELDING WHERE (AKTOR_ID = :aktorid AND ORGNUMMER = :orgnummer)",
                new MapSqlParameterSource()
                        .addValue("aktorid", aktoerId)
                        .addValue("orgnummer", orgnummer),
                (rs, rowNum) -> InntektsmeldingMeta
                        .builder()
                        .uuid(rs.getString("INNTEKTSMELDING_UUID"))
                        .aktorId(rs.getString("AKTOR_ID"))
                        .orgnummer(rs.getString("ORGNUMMER"))
                        .arbeidsgiverperiodeFom(rs.getDate("ARBEIDSGIVERPERIODE_FOM").toLocalDate())
                        .arbeidsgiverperiodeTom(rs.getDate("ARBEIDSGIVERPERIODE_TOM").toLocalDate())
                        .sakId(rs.getString("SAK_ID"))
                        .journalpostId(rs.getString("JOURNALPOST_ID"))
                        .behandlet(Optional.ofNullable(rs.getTimestamp("BEHANDLET")).map(Timestamp::toLocalDateTime).orElse(null))
                        .build());
    }
}
