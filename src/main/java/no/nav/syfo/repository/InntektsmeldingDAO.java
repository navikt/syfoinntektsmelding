package no.nav.syfo.repository;

import lombok.extern.slf4j.Slf4j;
import no.nav.syfo.domain.Periode;
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
        String uuid = UUID.randomUUID().toString();
        namedParameterJdbcTemplate.update(
                "INSERT INTO INNTEKTSMELDING(INNTEKTSMELDING_UUID, AKTOR_ID, SAK_ID, ORGNUMMER, JOURNALPOST_ID, BEHANDLET) " +
                        "VALUES(:uuid, :aktorid, :sakid, :orgnummer, :journalpostId, :behandlet)",

                new MapSqlParameterSource()
                        .addValue("uuid", uuid)
                        .addValue("aktorid", inntektsmeldingMeta.getAktorId())
                        .addValue("orgnummer", inntektsmeldingMeta.getOrgnummer())
                        .addValue("sakid", inntektsmeldingMeta.getSakId())
                        .addValue("journalpostId", inntektsmeldingMeta.getJournalpostId())
                        .addValue("behandlet", inntektsmeldingMeta.getBehandlet())
        );

        inntektsmeldingMeta.getArbeidsgiverperioder().forEach(ap -> namedParameterJdbcTemplate.update(
                "INSERT INTO ARBEIDSGIVERPERIODE(PERIODE_UUID, INNTEKTSMELDING_UUID, FOM, TOM) VALUES(:periodeUuid, :inntektsmeldingUuid, :fom, :tom)",
                new MapSqlParameterSource()
                        .addValue("periodeUuid", UUID.randomUUID().toString())
                        .addValue("inntektsmeldingUuid", uuid)
                        .addValue("fom", ap.getFom())
                        .addValue("tom", ap.getTom())
        ));
    }

    public List<InntektsmeldingMeta> finnBehandledeInntektsmeldinger(String aktoerId) {
        return namedParameterJdbcTemplate.query("SELECT * FROM INNTEKTSMELDING WHERE (AKTOR_ID = :aktorid)",
                new MapSqlParameterSource()
                        .addValue("aktorid", aktoerId),
                (rs, rowNum) -> {
                    String inntektsmelding_uuid = rs.getString("INNTEKTSMELDING_UUID");

                    return InntektsmeldingMeta
                            .builder()
                            .uuid(inntektsmelding_uuid)
                            .aktorId(rs.getString("AKTOR_ID"))
                            .orgnummer(rs.getString("ORGNUMMER"))
                            .arbeidsgiverperioder(hentPerioder(inntektsmelding_uuid))
                            .sakId(rs.getString("SAK_ID"))
                            .journalpostId(rs.getString("JOURNALPOST_ID"))
                            .behandlet(Optional.ofNullable(rs.getTimestamp("BEHANDLET")).map(Timestamp::toLocalDateTime).orElse(null))
                            .build();
                }
        );
    }

    private List<Periode> hentPerioder(String inntektsmeldingUuid) {
        return namedParameterJdbcTemplate.query("SELECT * FROM ARBEIDSGIVERPERIODE WHERE (INNTEKTSMELDING_UUID = :inntektsmeldingUUID)",
                new MapSqlParameterSource()
                        .addValue("inntektsmeldingUUID", inntektsmeldingUuid),
                (rs, rowNum) ->
                        Periode.builder()
                                .fom(rs.getDate("FOM").toLocalDate())
                                .tom(rs.getDate("TOM").toLocalDate())
                                .build());
    }
}
